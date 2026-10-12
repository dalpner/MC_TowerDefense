package io.github.dalpner.mctd.core;

import static io.github.dalpner.mctd.core.CreepKind.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Deterministic wave generator. The same seed always produces the same sequence of elements, kinds and
 * abilities, so the upcoming waves can be shown to the players in advance.
 * Every tenth wave is a boss wave and the last wave is the final boss.
 */
public final class WaveGenerator {
    public static final int DEFAULT_WAVES = 50;

    private static final CreepKind[] EARLY = {NORMAL, NORMAL, FAST, MASS, NORMAL, AIR, FAST, MASS, NORMAL};
    private static final CreepKind[] CYCLE = {NORMAL, FAST, MASS, AIR, NORMAL, FAST, MASS, AIR, NORMAL};

    private final long seed;
    private final Difficulty difficulty;
    private final int maxWaves;
    private final List<Element> order = new ArrayList<>();
    private final SplittableRandom orderRandom;

    public WaveGenerator(long seed, Difficulty difficulty, int maxWaves) {
        this.seed = seed;
        this.difficulty = difficulty;
        this.maxWaves = maxWaves;
        this.orderRandom = new SplittableRandom(seed);
    }

    public int maxWaves() {
        return maxWaves;
    }

    // ---------------------------------------------------------------- formulas

    /** Hit points of a normal creep in wave {@code n} before difficulty and kind multipliers. */
    public static double baseHp(int n) {
        return 20.0 * Math.pow(1.13, n - 1) + 8.0 * (n - 1);
    }

    /** Gold for killing a normal creep in wave {@code n}. */
    public static int baseBounty(int n) {
        return 2 + (int) Math.round(n * 0.6);
    }

    /** Gold for clearing wave {@code n}. */
    public static int clearBonus(int n) {
        return 10 + 3 * n;
    }

    /** Whether starting wave {@code n} grants an element essence. */
    public static boolean grantsEssence(int n) {
        return n >= 5 && n <= 45 && n % 4 == 1;
    }

    /** Build time in ticks before wave {@code n} starts on its own. */
    public static int buildTicks(int n) {
        if (n <= 1) {
            return 45 * 20;
        }
        return n % 10 == 0 ? 30 * 20 : 20 * 20;
    }

    // ---------------------------------------------------------------- waves

    public WaveDef wave(int n) {
        CreepKind kind = kindOf(n);
        Element element = kind == FINAL_BOSS ? null : elementOf(n);
        Set<Ability> abilities = abilitiesOf(n, kind);
        double base = baseHp(n) * difficulty.hpMultiplier();
        double hp = base * kind.hpMultiplier();
        if (kind != FINAL_BOSS && !abilities.isEmpty()) {
            hp *= 1.0 - 0.08 * abilities.size();
        }
        int bounty = Math.max(1, (int) Math.round(baseBounty(n) * kind.bountyMultiplier()));
        double armor = abilities.contains(Ability.ARMORED) ? base * 0.05 : 0;
        MobTable.Body body = MobTable.body(element, kind);
        CreepSpec spec = new CreepSpec(n, element, kind, abilities, hp, kind.speed(), bounty,
                kind.livesCost(), body.mob(), body.scale(), armor, false);
        return new WaveDef(n, spec, kind.count(), kind.spawnInterval());
    }

    public CreepKind kindOf(int n) {
        if (n == maxWaves || (n > maxWaves && n % 50 == 0)) {
            return FINAL_BOSS;
        }
        if (n % 10 == 0) {
            return BOSS;
        }
        if (n < 10) {
            return EARLY[n - 1];
        }
        return CYCLE[(n % 10) - 1];
    }

    /** Element of wave {@code n}. Every block of six waves contains each element exactly once. */
    public Element elementOf(int n) {
        while (order.size() < n) {
            List<Element> block = new ArrayList<>(List.of(Element.values()));
            for (int i = block.size() - 1; i > 0; i--) {
                Collections.swap(block, i, orderRandom.nextInt(i + 1));
            }
            if (!order.isEmpty() && block.get(0) == order.get(order.size() - 1)) {
                Collections.swap(block, 0, block.size() - 1);
            }
            order.addAll(block);
        }
        return order.get(n - 1);
    }

    public Set<Ability> abilitiesOf(int n, CreepKind kind) {
        if (kind == FINAL_BOSS) {
            return Collections.emptySet();
        }
        if (n <= 10) {
            return Collections.emptySet();
        }
        double first;
        double second;
        if (n <= 20) {
            first = 0.5;
            second = 0.0;
        } else if (n <= 30) {
            first = 0.7;
            second = 0.1;
        } else if (n <= 40) {
            first = 0.85;
            second = 0.25;
        } else {
            first = 1.0;
            second = 0.4;
        }
        List<Ability> pool = new ArrayList<>(List.of(Ability.values()));
        if (kind == MASS) {
            pool.remove(Ability.SPLIT);
        }
        SplittableRandom random = new SplittableRandom(seed ^ (n * 0x9E3779B97F4A7C15L));
        EnumSet<Ability> result = EnumSet.noneOf(Ability.class);
        if (random.nextDouble() < first) {
            result.add(pool.remove(random.nextInt(pool.size())));
        }
        if (random.nextDouble() < second) {
            result.add(pool.remove(random.nextInt(pool.size())));
        }
        return result;
    }

    /**
     * The element guardian that appears when an element is bought. It grows stronger with the upcoming wave
     * {@code n} and with the element level that was just reached.
     */
    public CreepSpec elementalSpec(Element element, int n, int level) {
        int wave = Math.max(1, n);
        double levelFactor = 1.0 + 0.25 * (Math.max(1, level) - 1);
        double hp = baseHp(wave) * difficulty.hpMultiplier() * ELEMENTAL.hpMultiplier() * levelFactor;
        int bounty = (int) Math.round(baseBounty(wave) * ELEMENTAL.bountyMultiplier() * levelFactor);
        MobTable.Body body = MobTable.body(element, ELEMENTAL);
        return new CreepSpec(0, element, ELEMENTAL, Collections.emptySet(), hp, ELEMENTAL.speed(), bounty,
                ELEMENTAL.livesCost(), body.mob(), body.scale(), 0, false);
    }
}
