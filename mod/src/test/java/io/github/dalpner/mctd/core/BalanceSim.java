package io.github.dalpner.mctd.core;

import io.github.dalpner.mctd.core.sim.Creep;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.GameConfig;
import io.github.dalpner.mctd.core.sim.GameListener;
import io.github.dalpner.mctd.core.sim.Tower;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Plays complete games with a simple greedy bot to check the difficulty curve. The bot builds and upgrades
 * whatever gives the most estimated damage per gold at the best free spot and buys elements following a
 * fixed plan. A human who uses element counters, slows and support towers should do better.
 */
public final class BalanceSim {

    public record Result(GameConfig config, boolean victory, int lastWave, int livesLeft, int towers,
                         long goldEarned, Map<Integer, Integer> livesLostPerWave, double minutes,
                         Map<String, Integer> towerTypes, double rawDps) {
    }

    public static final Element[][] PLANS = {
            {Element.FIRE, Element.WATER, Element.NATURE, Element.FIRE, Element.WATER, Element.NATURE,
                    Element.FIRE, Element.WATER, Element.NATURE, Element.LIGHT, Element.EARTH, Element.DARKNESS},
            {Element.LIGHT, Element.DARKNESS, Element.EARTH, Element.LIGHT, Element.DARKNESS, Element.EARTH,
                    Element.LIGHT, Element.DARKNESS, Element.EARTH, Element.WATER, Element.FIRE, Element.NATURE},
            {Element.EARTH, Element.FIRE, Element.EARTH, Element.FIRE, Element.EARTH, Element.FIRE,
                    Element.LIGHT, Element.LIGHT, Element.LIGHT, Element.NATURE, Element.NATURE, Element.NATURE},
    };

    public static void main(String[] args) {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        boolean verbose = args.length > 1 && args[1].equals("-v");
        for (Difficulty difficulty : Difficulty.values()) {
            for (ArenaLayout layout : ArenaLayout.values()) {
                for (int plan = 0; plan < PLANS.length; plan++) {
                    for (int seed = 1; seed <= seeds; seed++) {
                        Result r = play(GameConfig.of(difficulty, layout, seed * 7919L + plan), PLANS[plan]);
                        System.out.println(format(r, plan, verbose));
                    }
                }
            }
        }
    }

    public static String format(Result r, int plan, boolean verbose) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%-6s %-10s plan%d seed%-6d %-7s wave %2d lives %3d towers %3d gold %7d %5.1f min",
                r.config().difficulty().id(), r.config().layout().id(), plan, r.config().seed(),
                r.victory() ? "WIN" : "LOSS", r.lastWave(), r.livesLeft(), r.towers(), r.goldEarned(), r.minutes()));
        sb.append(String.format(Locale.ROOT, " dps %6.0f", r.rawDps()));
        sb.append("  leaks ").append(r.livesLostPerWave());
        if (verbose) {
            sb.append("\n        towers ").append(r.towerTypes());
        }
        return sb.toString();
    }

    public static Result play(GameConfig config, Element[] plan) {
        Game game = new Game(config);
        Map<Integer, Integer> lost = new TreeMap<>();
        game.setListener(new GameListener() {
            @Override
            public void creepLeaked(Creep creep) {
                int wave = creep.spec().wave() == 0 ? 0 : creep.spec().wave();
                lost.merge(wave, creep.spec().livesCost(), Integer::sum);
            }
        });
        Bot bot = new Bot(game, plan);
        long limit = 20L * 60 * 60 * 3;
        while (!game.isOver() && game.ticks() < limit) {
            if (game.ticks() % 20 == 0) {
                bot.act();
            }
            game.tick();
        }
        Map<String, Integer> types = new TreeMap<>();
        for (Tower t : game.towers()) {
            types.merge(t.type().id() + "@" + t.tier(), 1, Integer::sum);
        }
        double dps = 0;
        for (Tower t : game.towers()) {
            dps += t.stats().dps() * (1 + t.buffDamage()) * (1 + t.buffSpeed());
        }
        return new Result(config, game.phase() == Game.Phase.VICTORY, game.currentWave(), game.lives(),
                game.towers().size(), game.goldEarned(), lost, game.ticks() / 20.0 / 60.0, types, dps);
    }

    /** Greedy bot. */
    static final class Bot {
        private final Game game;
        private final Element[] plan;
        private int planIndex;
        private final ArenaLayout layout;
        private final List<int[]> cells;
        private final Map<Integer, List<int[]>> cellsByRange = new HashMap<>();
        private final Map<Long, Double> coverage = new HashMap<>();

        Bot(Game game, Element[] plan) {
            this.game = game;
            this.plan = plan;
            this.layout = game.config().layout();
            this.cells = layout.buildableCells();
        }

        void act() {
            while (game.essences() > 0 && planIndex < plan.length && game.towers().size() >= 4) {
                game.buyElement(plan[planIndex]);
                planIndex++;
            }
            for (int guard = 0; guard < 40; guard++) {
                Option best = null;
                Option bestAffordable = null;
                for (Option o : options()) {
                    if (best == null || o.score > best.score) {
                        best = o;
                    }
                    if (o.cost <= game.gold() && (bestAffordable == null || o.score > bestAffordable.score)) {
                        bestAffordable = o;
                    }
                }
                if (bestAffordable == null) {
                    return;
                }
                boolean desperate = game.towers().size() < 3;
                if (!desperate && bestAffordable.score < best.score * 0.6) {
                    return;
                }
                bestAffordable.run.run();
            }
        }

        private record Option(double score, int cost, Runnable run) {
        }

        private List<Option> options() {
            List<Option> list = new ArrayList<>();
            int[] levels = game.levels();
            double air = 0;
            double ground = 0;
            for (Tower t : game.towers()) {
                double v = effectiveDps(t.type(), t.tier()) * coverage(new int[]{t.cellX(), t.cellZ()}, t.stats().range);
                ground += v;
                if (t.type().hitsAir()) {
                    air += v;
                }
            }
            // keep enough anti-air: ground-only towers lose value while air defence lags behind
            double groundOnlyFactor = air < ground * 0.6 ? 0.2 : 1.0;
            for (TowerType type : Towers.all()) {
                if (type.isSupportOnly() || !type.unlocked(levels, 1)) {
                    continue;
                }
                TowerType.Tier s = type.tier(1);
                int[] cell = bestFreeCell(s.range);
                if (cell == null) {
                    continue;
                }
                double value = effectiveDps(type, 1) * coverage(cell, s.range);
                if (!type.hitsAir()) {
                    value *= groundOnlyFactor;
                }
                list.add(new Option(value / s.cost, s.cost, () -> game.build(type, cell[0], cell[1])));
            }
            for (Tower t : game.towers()) {
                if (t.maxed() || !t.type().unlocked(levels, t.tier() + 1)) {
                    continue;
                }
                TowerType.Tier next = t.type().tier(t.tier() + 1);
                int[] cell = {t.cellX(), t.cellZ()};
                double gain = effectiveDps(t.type(), t.tier() + 1) * coverage(cell, next.range)
                        - effectiveDps(t.type(), t.tier()) * coverage(cell, t.stats().range);
                if (!t.type().hitsAir()) {
                    gain *= groundOnlyFactor;
                }
                list.add(new Option(gain / next.cost, next.cost, () -> game.upgrade(t)));
            }
            return list;
        }

        private int[] bestFreeCell(double range) {
            int key = (int) Math.round(range * 10);
            List<int[]> sorted = cellsByRange.computeIfAbsent(key, k -> {
                List<int[]> copy = new ArrayList<>(cells);
                copy.sort(Comparator.comparingDouble(c -> -coverage(c, range)));
                return copy;
            });
            for (int[] c : sorted) {
                if (game.towerAt(c[0], c[1]) == null) {
                    return c;
                }
            }
            return null;
        }

        private double coverage(int[] cell, double range) {
            long key = ((long) cell[0] << 40) | ((long) cell[1] << 20) | Math.round(range * 10);
            return coverage.computeIfAbsent(key,
                    k -> layout.path().lengthWithin(cell[0] + 0.5, cell[1] + 0.5, range));
        }

        static double effectiveDps(TowerType type, int tier) {
            TowerType.Tier s = type.tier(tier);
            double dps = s.dps();
            switch (type.style()) {
                case SPLASH -> dps *= 1 + s.splashFactor * Math.min(3, 1 + s.splashRadius * 0.5);
                case MULTI -> dps *= Math.min(s.targets, 4) * 0.8;
                case CHAIN -> {
                    double sum = 0;
                    double f = 1;
                    for (int i = 0; i <= Math.min(s.targets, 4); i++) {
                        sum += f;
                        f *= s.chainDecay;
                    }
                    dps *= sum * 0.85;
                }
                case PULSE -> dps *= 3.0;
                case RAMP -> dps *= 1 + Math.min(s.rampMax, s.ramp * 12);
                default -> {
                }
            }
            dps *= 1 + s.burn * 1.5;
            dps *= 1 + s.poison * s.poisonStacks * 0.6;
            dps *= 1 + s.disease * 2.5;
            dps *= 1 + s.slow * 0.8;
            dps *= 1 + s.curse * 0.8;
            dps *= 1 + s.critChance * (s.critMultiplier - 1);
            dps *= 1 + s.distanceBonus * 0.5;
            dps *= 1 + s.execute * 1.5;
            dps *= 1 + s.percentHp * 15;
            dps *= 1 + s.stunChance * 0.5;
            if (!type.hitsAir()) {
                dps *= 0.85;
            }
            if (type.damageElement() != null) {
                dps *= 1.08;
            }
            return dps;
        }
    }
}
