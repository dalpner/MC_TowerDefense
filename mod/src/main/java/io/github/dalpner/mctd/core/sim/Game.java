package io.github.dalpner.mctd.core.sim;

import io.github.dalpner.mctd.core.Ability;
import io.github.dalpner.mctd.core.AttackStyle;
import io.github.dalpner.mctd.core.CreepSpec;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.PathLine;
import io.github.dalpner.mctd.core.TargetMode;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.Towers;
import io.github.dalpner.mctd.core.WaveDef;
import io.github.dalpner.mctd.core.WaveGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * The complete tower defense simulation of one game, independent of Minecraft. Call {@link #tick()} 20
 * times per second; everything visible is reported to the {@link GameListener}.
 */
public final class Game {

    public enum Phase {
        /** Countdown to the next wave. */
        BUILD,
        /** At least one wave is spawning or has creeps alive. */
        WAVE,
        VICTORY,
        DEFEAT
    }

    public static final double SELL_REFUND = 0.75;
    public static final double INTEREST_RATE = 0.03;
    public static final double REGEN_PER_SECOND = 0.02;
    public static final double BOSS_REGEN_PER_SECOND = 0.006;
    public static final double HEAL_SHARE = 0.06;
    public static final double BOSS_HEAL_SHARE = 0.015;
    public static final int HEAL_INTERVAL = 60;
    public static final double HEAL_RADIUS = 4.0;
    public static final int REVIVE_TICKS = 40;
    public static final double REVIVE_HP = 0.5;
    public static final double ARMOR_MIN_SHARE = 0.3;
    public static final double MAX_LATERAL = 0.8;
    public static final double MAX_AURA = 1.0;
    public static final double MAX_BOUNTY_BONUS = 1.5;
    public static final double BOSS_PERCENT_HP_FACTOR = 0.5;

    private record PendingSpawn(long at, CreepSpec spec) {
    }

    private final GameConfig config;
    private final PathLine path;
    private final WaveGenerator waves;
    private final SplittableRandom random;
    private final double[] tmp = new double[4];
    private GameListener listener = new GameListener() {
    };

    private int gold;
    private int lives;
    private int essences;
    private final int[] levels = new int[Element.values().length];
    private int nextWave = 1;
    private int essenceGrantedUpTo;
    private int countdown;
    private Phase phase = Phase.BUILD;
    private int phaseId;
    private boolean endless;
    private long tick;
    private int lastEarlyBonus;

    private final List<Creep> creeps = new ArrayList<>();
    private final List<PendingSpawn> pending = new ArrayList<>();
    private final TreeMap<Integer, Integer> remaining = new TreeMap<>();
    private final Map<Long, Tower> towers = new LinkedHashMap<>();
    private int nextCreepId = 1;
    private int nextTowerId = 1;

    private int kills;
    private int leaks;
    private long goldEarned;
    private double damageDealt;

    public Game(GameConfig config) {
        this.config = config;
        this.path = config.layout().path();
        this.waves = new WaveGenerator(config.seed(), config.difficulty(), config.maxWaves());
        this.random = new SplittableRandom(config.seed() * 31 + 7);
        this.gold = config.difficulty().startGold();
        this.lives = config.difficulty().lives();
        this.essences = 1;
        this.countdown = WaveGenerator.buildTicks(1);
    }

    public void setListener(GameListener listener) {
        this.listener = listener == null ? new GameListener() {
        } : listener;
    }

    // ================================================================== main loop

    public void tick() {
        if (isOver()) {
            return;
        }
        tick++;
        spawnPending();
        tickCreeps();
        if (isOver()) {
            return;
        }
        tickTowers();
        creeps.removeIf(Creep::removed);
        updatePhase();
    }

    private void spawnPending() {
        Iterator<PendingSpawn> it = pending.iterator();
        while (it.hasNext()) {
            PendingSpawn p = it.next();
            if (p.at() <= tick) {
                it.remove();
                spawn(p.spec(), 0, Double.NaN);
            }
        }
    }

    private Creep spawn(CreepSpec spec, double distance, double lateral) {
        Creep c = new Creep(nextCreepId++, spec);
        c.distance = distance;
        c.lateral = Double.isNaN(lateral) ? (random.nextDouble() * 2 - 1) * MAX_LATERAL : clampLateral(lateral);
        c.updatePosition(path, tmp);
        creeps.add(c);
        listener.creepSpawned(c);
        return c;
    }

    private static double clampLateral(double lateral) {
        return Math.max(-MAX_LATERAL, Math.min(MAX_LATERAL, lateral));
    }

    private void tickCreeps() {
        int size = creeps.size();
        for (int i = 0; i < size; i++) {
            Creep c = creeps.get(i);
            if (!c.removed) {
                tickCreep(c);
                if (isOver()) {
                    return;
                }
            }
        }
    }

    private void tickCreep(Creep c) {
        if (c.reviveTicks > 0) {
            if (--c.reviveTicks == 0) {
                c.hp = c.maxHp * REVIVE_HP;
                listener.creepRose(c);
            }
            return;
        }
        if (c.burnTicks > 0) {
            c.burnTicks--;
            damageOverTime(c, c.burnDps, c.burnSource);
            if (!c.targetable()) {
                return;
            }
        }
        if (c.poisonTicks > 0) {
            c.poisonTicks--;
            damageOverTime(c, c.poisonDps * c.poisonStacks, c.poisonSource);
            if (c.poisonTicks == 0) {
                c.poisonStacks = 0;
            }
            if (!c.targetable()) {
                return;
            }
        }
        if (c.diseaseTicks > 0) {
            c.diseaseTicks--;
            damageOverTime(c, c.diseaseDps, c.diseaseSource);
            if (!c.targetable()) {
                return;
            }
            if ((tick + c.id()) % 20 == 0) {
                spreadDisease(c);
            }
        }
        if (c.has(Ability.REGEN) && c.hp < c.maxHp) {
            double rate = c.spec().bossLike() ? BOSS_REGEN_PER_SECOND : REGEN_PER_SECOND;
            c.hp = Math.min(c.maxHp, c.hp + c.maxHp * rate / 20.0);
        }
        if (c.has(Ability.HEALER) && --c.healTimer <= 0) {
            c.healTimer = HEAL_INTERVAL;
            heal(c);
        }
        if (c.slowTicks > 0 && --c.slowTicks == 0) {
            c.slow = 0;
        }
        if (c.curseTicks > 0 && --c.curseTicks == 0) {
            c.curse = 0;
        }
        if (c.stunTicks > 0) {
            c.stunTicks--;
            return;
        }
        double speed = c.spec().speed() * (1.0 - c.slow);
        c.distance += speed / 20.0;
        if (c.distance >= path.length()) {
            leak(c);
            return;
        }
        c.updatePosition(path, tmp);
    }

    private void damageOverTime(Creep c, double dps, Tower source) {
        if (dps <= 0) {
            return;
        }
        double amount = dps * elementMultiplier(source, c) * (1.0 + c.curse) / 20.0;
        damage(c, amount, source, false);
    }

    private void spreadDisease(Creep c) {
        double r2 = c.diseaseRadius * c.diseaseRadius;
        for (Creep o : creeps) {
            if (o != c && o.targetable() && o.diseaseTicks <= 0 && dist2(c, o) <= r2) {
                o.diseaseDps = c.diseaseDps;
                o.diseaseTicks = c.diseaseTicks;
                o.diseaseRadius = c.diseaseRadius;
                o.diseaseSource = c.diseaseSource;
            }
        }
    }

    private void heal(Creep healer) {
        List<Creep> healed = new ArrayList<>();
        double r2 = HEAL_RADIUS * HEAL_RADIUS;
        for (Creep o : creeps) {
            if (o.targetable() && o.hp < o.maxHp && dist2(healer, o) <= r2) {
                double share = o.spec().bossLike() ? BOSS_HEAL_SHARE : HEAL_SHARE;
                o.hp = Math.min(o.maxHp, o.hp + o.maxHp * share);
                healed.add(o);
            }
        }
        if (!healed.isEmpty()) {
            listener.creepsHealed(healer, healed);
        }
    }

    private void leak(Creep c) {
        c.removed = true;
        c.leaked = true;
        leaks++;
        lives = Math.max(0, lives - c.spec().livesCost());
        listener.creepLeaked(c);
        if (lives <= 0) {
            defeat();
            return;
        }
        creepFinished(c);
    }

    private void tickTowers() {
        for (Tower t : towers.values()) {
            TowerType.Tier s = t.stats();
            if (t.type().isSupportOnly() || s.cooldown <= 0) {
                continue;
            }
            t.cooldown -= 1.0 + t.buffSpeed;
            if (t.cooldown > 0) {
                continue;
            }
            if (attack(t, s)) {
                t.cooldown += s.cooldown;
            } else {
                t.cooldown = 0;
            }
        }
    }

    private void updatePhase() {
        boolean active = !pending.isEmpty() || !remaining.isEmpty();
        if (active) {
            if (phase != Phase.WAVE) {
                phase = Phase.WAVE;
                phaseId++;
            }
            return;
        }
        if (!endless && nextWave > config.maxWaves()) {
            if (creeps.isEmpty()) {
                phase = Phase.VICTORY;
                phaseId++;
                listener.gameOver(true);
            }
            return;
        }
        if (phase != Phase.BUILD) {
            phase = Phase.BUILD;
            phaseId++;
            countdown = WaveGenerator.buildTicks(nextWave);
            listener.countdownStarted(nextWave, countdown);
            return;
        }
        if (--countdown <= 0) {
            startWave(nextWave);
        }
    }

    private void startWave(int n) {
        WaveDef def = waves.wave(n);
        long at = tick + 1;
        for (int i = 0; i < def.count(); i++) {
            pending.add(new PendingSpawn(at + (long) i * def.spawnInterval(), def.spec()));
        }
        remaining.merge(n, def.count(), Integer::sum);
        nextWave = n + 1;
        countdown = 0;
        if (phase != Phase.WAVE) {
            phase = Phase.WAVE;
            phaseId++;
        }
        listener.waveStarted(def);
        if (WaveGenerator.grantsEssence(n) && n > essenceGrantedUpTo) {
            essences++;
            essenceGrantedUpTo = n;
            listener.essenceGained(essences);
        }
    }

    private void defeat() {
        phase = Phase.DEFEAT;
        phaseId++;
        for (Creep c : creeps) {
            c.removed = true;
        }
        pending.clear();
        remaining.clear();
        listener.gameOver(false);
    }

    // ================================================================== combat

    private boolean attack(Tower t, TowerType.Tier s) {
        TowerType type = t.type();
        switch (type.style()) {
            case SINGLE, RAMP -> {
                Creep target = null;
                if (type.style() == AttackStyle.RAMP && t.rampTarget != null && t.rampTarget.targetable()
                        && canHit(type, t.rampTarget) && dist2(t, t.rampTarget) <= s.range * s.range) {
                    target = t.rampTarget;
                }
                if (target == null) {
                    target = selectTarget(t, s.range);
                }
                if (target == null) {
                    return false;
                }
                double dmg = s.damage;
                if (type.style() == AttackStyle.RAMP) {
                    if (t.rampTarget == target) {
                        t.rampStacks++;
                    } else {
                        t.rampTarget = target;
                        t.rampStacks = 0;
                    }
                    dmg *= 1.0 + Math.min(s.rampMax, s.ramp * t.rampStacks);
                }
                if (s.distanceBonus > 0) {
                    double d = Math.sqrt(dist2(t, target));
                    dmg *= 1.0 + s.distanceBonus * Math.min(1.0, d / s.range);
                }
                boolean crit = rollCrit(s);
                listener.towerAttacked(t, List.of(target));
                hit(t, s, target, dmg, crit);
                return true;
            }
            case MULTI -> {
                List<Creep> targets = selectTargets(t, s.range, s.targets);
                if (targets.isEmpty()) {
                    return false;
                }
                boolean crit = rollCrit(s);
                listener.towerAttacked(t, Collections.unmodifiableList(targets));
                for (Creep c : targets) {
                    hit(t, s, c, s.damage, crit);
                }
                return true;
            }
            case SPLASH -> {
                Creep target = selectTarget(t, s.range);
                if (target == null) {
                    return false;
                }
                List<Creep> hits = new ArrayList<>();
                hits.add(target);
                double r2 = s.splashRadius * s.splashRadius;
                for (Creep c : creeps) {
                    if (c != target && c.targetable() && canHit(type, c) && dist2(c, target) <= r2) {
                        hits.add(c);
                    }
                }
                boolean crit = rollCrit(s);
                listener.towerAttacked(t, Collections.unmodifiableList(hits));
                hit(t, s, target, s.damage, crit);
                for (int i = 1; i < hits.size(); i++) {
                    hit(t, s, hits.get(i), s.damage * s.splashFactor, crit);
                }
                return true;
            }
            case CHAIN -> {
                Creep target = selectTarget(t, s.range);
                if (target == null) {
                    return false;
                }
                List<Creep> chain = new ArrayList<>();
                chain.add(target);
                Creep current = target;
                for (int i = 0; i < s.targets; i++) {
                    Creep next = nearestUnhit(current, s.chainRange, chain, type);
                    if (next == null) {
                        break;
                    }
                    chain.add(next);
                    current = next;
                }
                boolean crit = rollCrit(s);
                listener.towerAttacked(t, Collections.unmodifiableList(chain));
                double dmg = s.damage;
                for (Creep c : chain) {
                    hit(t, s, c, dmg, crit);
                    dmg *= s.chainDecay;
                }
                return true;
            }
            case PULSE -> {
                List<Creep> all = inRange(t, s.range);
                if (all.isEmpty()) {
                    return false;
                }
                listener.towerAttacked(t, Collections.unmodifiableList(all));
                for (Creep c : all) {
                    hit(t, s, c, s.damage, false);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean rollCrit(TowerType.Tier s) {
        return s.critChance > 0 && random.nextDouble() < s.critChance;
    }

    private void hit(Tower t, TowerType.Tier s, Creep c, double base, boolean crit) {
        if (!c.targetable()) {
            return;
        }
        double mult = elementMultiplier(t, c);
        double scaled = base * (1.0 + t.buffDamage);
        double dmg = scaled * (crit ? s.critMultiplier : 1.0) * mult * (1.0 + c.curse);
        if (s.percentHp > 0) {
            double share = c.spec().bossLike() ? s.percentHp * BOSS_PERCENT_HP_FACTOR : s.percentHp;
            dmg += c.maxHp * share * mult;
        }
        if (c.spec().armor() > 0) {
            dmg = Math.max(dmg * ARMOR_MIN_SHARE, dmg - c.spec().armor());
        }
        applyEffects(t, s, c, scaled);
        damage(c, dmg, t, crit);
        if (c.targetable() && s.execute > 0 && !c.spec().bossLike() && c.hp <= c.maxHp * s.execute) {
            damage(c, c.hp, t, false);
        }
    }

    private void applyEffects(Tower t, TowerType.Tier s, Creep c, double scaled) {
        boolean controllable = !c.has(Ability.IMMUNE);
        boolean boss = c.spec().bossLike();
        if (s.slow > 0 && controllable) {
            double amount = boss ? s.slow * 0.5 : s.slow;
            if (amount >= c.slow || c.slowTicks <= 0) {
                c.slow = amount;
                c.slowTicks = s.slowTicks;
            }
        }
        if (s.stunChance > 0 && controllable && !boss && random.nextDouble() < s.stunChance) {
            c.stunTicks = Math.max(c.stunTicks, s.stunTicks);
        }
        if (s.burn > 0) {
            double dps = scaled * s.burn;
            if (dps >= c.burnDps || c.burnTicks <= 0) {
                c.burnDps = dps;
                c.burnSource = t;
            }
            c.burnTicks = Math.max(c.burnTicks, s.burnTicks);
        }
        if (s.poison > 0) {
            double dps = scaled * s.poison;
            if (dps >= c.poisonDps || c.poisonTicks <= 0) {
                c.poisonDps = dps;
                c.poisonSource = t;
            }
            c.poisonStacks = Math.min(s.poisonStacks, c.poisonStacks + 1);
            c.poisonTicks = s.poisonTicks;
        }
        if (s.curse > 0 && (s.curse >= c.curse || c.curseTicks <= 0)) {
            c.curse = s.curse;
            c.curseTicks = s.curseTicks;
        }
        if (s.disease > 0) {
            double dps = scaled * s.disease;
            if (dps >= c.diseaseDps || c.diseaseTicks <= 0) {
                c.diseaseDps = dps;
                c.diseaseSource = t;
                c.diseaseRadius = s.diseaseRadius;
            }
            c.diseaseTicks = Math.max(c.diseaseTicks, s.diseaseTicks);
        }
        if (s.knockback > 0 && controllable && !boss) {
            c.distance = Math.max(0, c.distance - s.knockback * c.knockbackFactor);
            c.knockbackFactor *= 0.7;
            c.updatePosition(path, tmp);
        }
    }

    private void damage(Creep c, double amount, Tower source, boolean crit) {
        if (amount <= 0 || !c.targetable()) {
            return;
        }
        double dealt = Math.min(amount, c.hp);
        c.hp -= amount;
        damageDealt += dealt;
        if (source != null) {
            source.damageDealt += dealt;
        }
        listener.creepDamaged(c, source, amount, crit);
        if (c.hp <= 0) {
            kill(c, source);
        }
    }

    private void kill(Creep c, Tower killer) {
        if (c.removed) {
            return;
        }
        if (c.has(Ability.UNDEAD) && !c.risen) {
            c.risen = true;
            c.hp = 0;
            c.reviveTicks = REVIVE_TICKS;
            c.clearEffects();
            listener.creepFell(c);
            return;
        }
        c.removed = true;
        c.hp = 0;
        int reward = (int) Math.round(c.spec().bounty() * (1.0 + bountyBonusAt(c)));
        c.goldReward = reward;
        gold += reward;
        goldEarned += reward;
        kills++;
        if (killer != null) {
            killer.kills++;
        }
        listener.creepDied(c, killer);
        if (c.has(Ability.SPLIT) && !c.spec().splitChild()) {
            CreepSpec child = c.spec().toSplitChild();
            if (child.wave() > 0) {
                remaining.merge(child.wave(), 2, Integer::sum);
            }
            spawn(child, Math.max(0, c.distance - 0.6), c.lateral + 0.5);
            spawn(child, c.distance, c.lateral - 0.5);
        }
        creepFinished(c);
    }

    private void creepFinished(Creep c) {
        int w = c.spec().wave();
        Integer left = remaining.get(w);
        if (w <= 0 || left == null) {
            return;
        }
        if (left <= 1) {
            remaining.remove(w);
            waveCleared(w);
        } else {
            remaining.put(w, left - 1);
        }
    }

    private void waveCleared(int w) {
        if (isOver()) {
            return;
        }
        int bonus = WaveGenerator.clearBonus(w);
        int interest = (int) Math.min(30 + 5L * w, Math.floor(gold * INTEREST_RATE));
        int towerGold = 0;
        for (Tower t : towers.values()) {
            towerGold += t.stats().waveGold;
        }
        int total = bonus + interest + towerGold;
        gold += total;
        goldEarned += total;
        listener.waveCleared(w, bonus, interest, towerGold);
    }

    private double bountyBonusAt(Creep c) {
        double bonus = 0;
        for (Tower t : towers.values()) {
            TowerType.Tier s = t.stats();
            if (s.bountyBonus > 0 && dist2(t, c) <= s.range * s.range) {
                bonus += s.bountyBonus;
            }
        }
        return Math.min(MAX_BOUNTY_BONUS, bonus);
    }

    /** Element multiplier of a tower's damage against a creep. */
    public static double elementMultiplier(Tower t, Creep c) {
        if (t == null) {
            return 1.0;
        }
        Element creepElement = c.element();
        return Element.multiplier(t.type().damageElementAgainst(creepElement), creepElement);
    }

    // ================================================================== targeting

    private static boolean canHit(TowerType type, Creep c) {
        return c.flying() ? type.hitsAir() : type.hitsGround();
    }

    private static double dist2(Tower t, Creep c) {
        double dx = t.x() - c.x;
        double dz = t.z() - c.z;
        return dx * dx + dz * dz;
    }

    private static double dist2(Creep a, Creep b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static double dist2(Tower a, Tower b) {
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return dx * dx + dz * dz;
    }

    private static double score(TargetMode mode, Creep c, double d2) {
        return switch (mode) {
            case FIRST -> c.distance;
            case LAST -> -c.distance;
            case STRONGEST -> c.hp;
            case WEAKEST -> -c.hp;
            case CLOSEST -> -d2;
        };
    }

    private Creep selectTarget(Tower t, double range) {
        double r2 = range * range;
        Creep best = null;
        double bestScore = 0;
        for (Creep c : creeps) {
            if (!c.targetable() || !canHit(t.type(), c)) {
                continue;
            }
            double d2 = dist2(t, c);
            if (d2 > r2) {
                continue;
            }
            double sc = score(t.mode, c, d2);
            if (best == null || sc > bestScore) {
                best = c;
                bestScore = sc;
            }
        }
        return best;
    }

    private List<Creep> selectTargets(Tower t, double range, int n) {
        List<Creep> list = inRange(t, range);
        if (list.size() > n) {
            list.sort(Comparator.comparingDouble(c -> -score(t.mode, c, dist2(t, c))));
            return new ArrayList<>(list.subList(0, n));
        }
        return list;
    }

    private List<Creep> inRange(Tower t, double range) {
        double r2 = range * range;
        List<Creep> list = new ArrayList<>();
        for (Creep c : creeps) {
            if (c.targetable() && canHit(t.type(), c) && dist2(t, c) <= r2) {
                list.add(c);
            }
        }
        return list;
    }

    private Creep nearestUnhit(Creep from, double range, List<Creep> exclude, TowerType type) {
        double r2 = range * range;
        Creep best = null;
        double bestD2 = 0;
        for (Creep c : creeps) {
            if (!c.targetable() || !canHit(type, c) || exclude.contains(c)) {
                continue;
            }
            double d2 = dist2(from, c);
            if (d2 <= r2 && (best == null || d2 < bestD2)) {
                best = c;
                bestD2 = d2;
            }
        }
        return best;
    }

    private void recomputeAuras() {
        for (Tower t : towers.values()) {
            t.buffDamage = 0;
            t.buffSpeed = 0;
        }
        for (Tower src : towers.values()) {
            TowerType.Tier st = src.stats();
            if (!st.hasAura()) {
                continue;
            }
            double r2 = st.range * st.range;
            for (Tower t : towers.values()) {
                if (t != src && dist2(src, t) <= r2) {
                    t.buffDamage += st.auraDamage;
                    t.buffSpeed += st.auraSpeed;
                }
            }
        }
        for (Tower t : towers.values()) {
            t.buffDamage = Math.min(MAX_AURA, t.buffDamage);
            t.buffSpeed = Math.min(MAX_AURA, t.buffSpeed);
        }
    }

    // ================================================================== player actions

    public TdResult canBuild(TowerType type, int x, int z) {
        if (isOver()) {
            return TdResult.GAME_OVER;
        }
        if (!config.layout().isBuildable(x, z)) {
            return TdResult.NOT_BUILDABLE;
        }
        if (towers.containsKey(Tower.key(x, z))) {
            return TdResult.OCCUPIED;
        }
        if (!type.unlocked(levels, 1)) {
            return TdResult.LOCKED;
        }
        if (gold < type.tier(1).cost) {
            return TdResult.NOT_ENOUGH_GOLD;
        }
        return TdResult.OK;
    }

    /** Builds a tower; returns {@code null} if {@link #canBuild} fails. */
    public Tower build(TowerType type, int x, int z) {
        if (canBuild(type, x, z) != TdResult.OK) {
            return null;
        }
        Tower t = new Tower(nextTowerId++, type, x, z);
        int cost = type.tier(1).cost;
        gold -= cost;
        t.invested = cost;
        t.builtPhase = phaseId;
        towers.put(t.key(), t);
        recomputeAuras();
        return t;
    }

    public TdResult canUpgrade(Tower t) {
        if (isOver()) {
            return TdResult.GAME_OVER;
        }
        if (t.maxed()) {
            return TdResult.MAX_TIER;
        }
        if (!t.type().unlocked(levels, t.tier + 1)) {
            return TdResult.LOCKED;
        }
        if (gold < t.type().tier(t.tier + 1).cost) {
            return TdResult.NOT_ENOUGH_GOLD;
        }
        return TdResult.OK;
    }

    public TdResult upgrade(Tower t) {
        TdResult result = canUpgrade(t);
        if (result != TdResult.OK) {
            return result;
        }
        int cost = t.type().tier(t.tier + 1).cost;
        gold -= cost;
        t.invested += cost;
        t.tier++;
        recomputeAuras();
        return TdResult.OK;
    }

    /** Gold returned when selling; towers built during the current build phase are refunded completely. */
    public int sellValue(Tower t) {
        if (phase == Phase.BUILD && t.builtPhase == phaseId) {
            return t.invested;
        }
        return (int) Math.floor(t.invested * SELL_REFUND);
    }

    /** Sells a tower and returns the refunded gold, or -1 if the tower does not belong to this game. */
    public int sell(Tower t) {
        if (towers.get(t.key()) != t) {
            return -1;
        }
        int value = sellValue(t);
        gold += value;
        towers.remove(t.key());
        recomputeAuras();
        return value;
    }

    public void setTargetMode(Tower t, TargetMode mode) {
        t.mode = mode;
    }

    public TdResult buyElement(Element element) {
        if (isOver()) {
            return TdResult.GAME_OVER;
        }
        if (levels[element.ordinal()] >= Element.MAX_LEVEL) {
            return TdResult.ELEMENT_MAXED;
        }
        if (essences <= 0) {
            return TdResult.NO_ESSENCE;
        }
        essences--;
        int level = ++levels[element.ordinal()];
        Creep guardian = null;
        if (config.elementGuardians()) {
            guardian = spawn(waves.elementalSpec(element, nextWave, level), 0, 0);
        }
        listener.elementBought(element, level, guardian);
        return TdResult.OK;
    }

    /** Starts the next wave right away. Calling early during the countdown grants bonus gold. */
    public TdResult callNextWave() {
        lastEarlyBonus = 0;
        if (isOver()) {
            return TdResult.GAME_OVER;
        }
        if (!endless && nextWave > config.maxWaves()) {
            return TdResult.NO_MORE_WAVES;
        }
        if (!pending.isEmpty()) {
            return TdResult.STILL_SPAWNING;
        }
        if (phase == Phase.BUILD && countdown > 0) {
            lastEarlyBonus = earlyBonus(countdown, nextWave);
            gold += lastEarlyBonus;
            goldEarned += lastEarlyBonus;
        }
        startWave(nextWave);
        return TdResult.OK;
    }

    public static int earlyBonus(int ticksLeft, int wave) {
        return (ticksLeft / 20) * (1 + wave / 10);
    }

    /** After a victory: keep playing with ever stronger waves. */
    public boolean continueEndless() {
        if (phase != Phase.VICTORY) {
            return false;
        }
        endless = true;
        phase = Phase.BUILD;
        phaseId++;
        countdown = WaveGenerator.buildTicks(nextWave);
        listener.countdownStarted(nextWave, countdown);
        return true;
    }

    // ---- admin helpers

    /** Spawns an extra creep at the start of the path that does not belong to a wave. */
    public Creep spawnExtra(CreepSpec spec) {
        CreepSpec extra = spec.wave() == 0 ? spec : new CreepSpec(0, spec.element(), spec.kind(), spec.abilities(),
                spec.hp(), spec.speed(), spec.bounty(), spec.livesCost(), spec.mob(), spec.scale(), spec.armor(),
                spec.splitChild());
        return spawn(extra, 0, Double.NaN);
    }

    public void addGold(int amount) {
        gold = Math.max(0, gold + amount);
    }

    public void addEssences(int amount) {
        essences = Math.max(0, essences + amount);
    }

    public void setLives(int amount) {
        lives = Math.max(1, amount);
    }

    // ================================================================== queries

    public GameConfig config() {
        return config;
    }

    public PathLine path() {
        return path;
    }

    public WaveGenerator waves() {
        return waves;
    }

    public int gold() {
        return gold;
    }

    public int lives() {
        return lives;
    }

    public int essences() {
        return essences;
    }

    public int level(Element element) {
        return levels[element.ordinal()];
    }

    public int[] levels() {
        return levels.clone();
    }

    /** Number of the next wave that will start. */
    public int nextWave() {
        return nextWave;
    }

    /** Number of the most recently started wave (0 before the first wave). */
    public int currentWave() {
        return nextWave - 1;
    }

    public int maxWaves() {
        return config.maxWaves();
    }

    public boolean endless() {
        return endless;
    }

    public Phase phase() {
        return phase;
    }

    public boolean isOver() {
        return phase == Phase.VICTORY || phase == Phase.DEFEAT;
    }

    /** Ticks until the next wave starts on its own (only meaningful in the build phase). */
    public int countdown() {
        return countdown;
    }

    public long ticks() {
        return tick;
    }

    public int lastEarlyBonus() {
        return lastEarlyBonus;
    }

    public List<Creep> creeps() {
        return Collections.unmodifiableList(creeps);
    }

    /** Creeps of running waves that are alive or still waiting to spawn. */
    public int waveCreepsLeft() {
        int sum = 0;
        for (int v : remaining.values()) {
            sum += v;
        }
        return sum;
    }

    public boolean spawning() {
        return !pending.isEmpty();
    }

    public Collection<Tower> towers() {
        return Collections.unmodifiableCollection(towers.values());
    }

    public Tower towerAt(int x, int z) {
        return towers.get(Tower.key(x, z));
    }

    public int kills() {
        return kills;
    }

    public int leaks() {
        return leaks;
    }

    public long goldEarned() {
        return goldEarned;
    }

    public double damageDealt() {
        return damageDealt;
    }

    // ================================================================== persistence

    public record TowerState(int x, int z, String type, int tier, String mode, int kills, double damage,
                             int invested) {
    }

    public record Snapshot(GameConfig config, int gold, int lives, int essences, int[] levels, int nextWave,
                           int essenceGrantedUpTo, boolean endless, String phase, List<TowerState> towers,
                           int kills, int leaks, long goldEarned, double damageDealt) {
    }

    /**
     * Captures the game so it can be restored later. Creeps are not saved: a wave that is running when the
     * snapshot is taken will be played again after restoring.
     */
    public Snapshot snapshot() {
        int resume = remaining.isEmpty() ? nextWave : Math.min(nextWave, remaining.firstKey());
        String phaseName = isOver() ? phase.name() : Phase.BUILD.name();
        List<TowerState> states = new ArrayList<>();
        for (Tower t : towers.values()) {
            states.add(new TowerState(t.cellX(), t.cellZ(), t.type().id(), t.tier, t.mode.id(), t.kills,
                    t.damageDealt, t.invested));
        }
        return new Snapshot(config, gold, lives, essences, levels.clone(), resume, essenceGrantedUpTo, endless,
                phaseName, states, kills, leaks, goldEarned, damageDealt);
    }

    public static Game restore(Snapshot s) {
        Game g = new Game(s.config());
        g.gold = s.gold();
        g.lives = s.lives();
        g.essences = s.essences();
        for (int i = 0; i < g.levels.length && i < s.levels().length; i++) {
            g.levels[i] = Math.max(0, Math.min(Element.MAX_LEVEL, s.levels()[i]));
        }
        g.nextWave = Math.max(1, s.nextWave());
        g.essenceGrantedUpTo = s.essenceGrantedUpTo();
        g.endless = s.endless();
        Phase phase;
        try {
            phase = Phase.valueOf(s.phase());
        } catch (IllegalArgumentException e) {
            phase = Phase.BUILD;
        }
        g.phase = phase == Phase.WAVE ? Phase.BUILD : phase;
        g.countdown = Math.max(30 * 20, WaveGenerator.buildTicks(g.nextWave));
        for (TowerState ts : s.towers()) {
            TowerType type = Towers.byId(ts.type());
            if (type == null || g.towers.containsKey(Tower.key(ts.x(), ts.z()))) {
                continue;
            }
            Tower t = new Tower(g.nextTowerId++, type, ts.x(), ts.z());
            t.tier = Math.max(1, Math.min(type.maxTier(), ts.tier()));
            t.mode = TargetMode.byId(ts.mode());
            t.kills = ts.kills();
            t.damageDealt = ts.damage();
            t.invested = ts.invested();
            t.builtPhase = -1;
            g.towers.put(t.key(), t);
        }
        g.kills = s.kills();
        g.leaks = s.leaks();
        g.goldEarned = s.goldEarned();
        g.damageDealt = s.damageDealt();
        g.recomputeAuras();
        return g;
    }
}
