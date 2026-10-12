package io.github.dalpner.mctd.core;

import io.github.dalpner.mctd.core.sim.Creep;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.GameConfig;
import io.github.dalpner.mctd.core.sim.GameListener;
import io.github.dalpner.mctd.core.sim.Tower;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Self-contained checks of the game core. They run without any test framework (see {@link #main}) and
 * are wrapped by a JUnit test for the Gradle build.
 */
public final class CoreSelfTest {
    private static int checks;

    public static void main(String[] args) {
        runAll();
        System.out.println("CoreSelfTest: all " + checks + " checks passed");
    }

    public static void runAll() {
        checks = 0;
        elementWheel();
        towerCatalogue();
        layouts();
        pathSmoothness();
        waveGenerator();
        economy();
        elementsAndGuardian();
        combatClearsFirstWave();
        undeadAndSplit();
        snapshotRoundTrip();
        defeat();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void elementWheel() {
        check(Element.multiplier(Element.LIGHT, Element.DARKNESS) == 2.0, "light beats darkness");
        check(Element.multiplier(Element.DARKNESS, Element.LIGHT) == 0.5, "darkness weak vs light");
        check(Element.multiplier(Element.WATER, Element.FIRE) == 2.0, "water beats fire");
        check(Element.multiplier(Element.EARTH, Element.LIGHT) == 2.0, "earth beats light");
        check(Element.multiplier(Element.LIGHT, Element.WATER) == 1.0, "light neutral vs water");
        check(Element.multiplier(null, Element.FIRE) == 1.0, "neutral attacker");
        check(Element.multiplier(Element.FIRE, null) == 1.0, "neutral defender");
        for (Element e : Element.values()) {
            int strong = 0;
            int weak = 0;
            for (Element o : Element.values()) {
                double m = Element.multiplier(e, o);
                if (m > 1) {
                    strong++;
                }
                if (m < 1) {
                    weak++;
                }
            }
            check(strong == 1 && weak == 1, e + " must have exactly one strength and one weakness");
            check(e.beats().beatenBy() == e, "wheel is consistent for " + e);
        }
    }

    private static void towerCatalogue() {
        check(Towers.all().size() == 43, "43 towers, got " + Towers.all().size());
        check(Towers.byCategory(TowerType.Category.BASIC).size() == 2, "2 basic towers");
        check(Towers.byCategory(TowerType.Category.ELEMENT).size() == 6, "6 element towers");
        check(Towers.byCategory(TowerType.Category.DUAL).size() == 15, "15 dual towers");
        check(Towers.byCategory(TowerType.Category.TRIPLE).size() == 20, "20 triple towers");
        Element[] all = Element.values();
        for (int a = 0; a < all.length; a++) {
            check(Towers.forElements(EnumSet.of(all[a])) != null, "element tower for " + all[a]);
            for (int b = a + 1; b < all.length; b++) {
                check(Towers.forElements(EnumSet.of(all[a], all[b])) != null,
                        "dual tower for " + all[a] + "+" + all[b]);
                for (int c = b + 1; c < all.length; c++) {
                    check(Towers.forElements(EnumSet.of(all[a], all[b], all[c])) != null,
                            "triple tower for " + all[a] + "+" + all[b] + "+" + all[c]);
                }
            }
        }
        Set<String> ids = new HashSet<>();
        for (TowerType t : Towers.all()) {
            check(ids.add(t.id()), "unique id " + t.id());
            check(t.id().matches("[a-z_]+"), "valid id " + t.id());
            check(t.maxTier() == 3, t.id() + " has 3 tiers");
            int expectedElements = switch (t.category()) {
                case BASIC -> 0;
                case ELEMENT -> 1;
                case DUAL -> 2;
                case TRIPLE -> 3;
            };
            check(t.elements().size() == expectedElements, t.id() + " element count");
            check(t.damageElement() == null ? t.elements().isEmpty() : t.elements().contains(t.damageElement()),
                    t.id() + " damage element must be one of its elements");
            double lastDps = 0;
            for (int tier = 1; tier <= 3; tier++) {
                TowerType.Tier s = t.tier(tier);
                check(s.cost > 0, t.id() + " cost");
                check(s.range >= 4, t.id() + " range");
                if (!t.isSupportOnly()) {
                    check(s.damage > 0 && s.cooldown > 0, t.id() + " attacks");
                    check(s.dps() > lastDps, t.id() + " dps grows with tier");
                    lastDps = s.dps();
                } else {
                    check(s.hasAura(), t.id() + " support needs an aura");
                }
                if (t.style() == AttackStyle.SPLASH) {
                    check(s.splashRadius > 0 && s.splashFactor > 0, t.id() + " splash values");
                }
                if (t.style() == AttackStyle.MULTI || t.style() == AttackStyle.CHAIN) {
                    check(s.targets >= 1, t.id() + " targets");
                }
            }
            int[] levels = new int[6];
            check(t.unlocked(levels, 1) == t.elements().isEmpty(), t.id() + " lock state at level 0");
            for (Element e : t.elements()) {
                levels[e.ordinal()] = 2;
            }
            check(t.unlocked(levels, 2) && !t.unlocked(levels, 3) == !t.elements().isEmpty(),
                    t.id() + " tier requirements");
        }
    }

    private static void layouts() {
        for (ArenaLayout layout : ArenaLayout.values()) {
            for (int[] wp : layout.waypoints()) {
                check(layout.isPath(wp[0], wp[1]), layout + " waypoint on path " + wp[0] + "," + wp[1]);
            }
            for (int i = 0; i < layout.size(); i++) {
                check(layout.cell(i, 0) == ArenaLayout.BORDER && layout.cell(0, i) == ArenaLayout.BORDER,
                        layout + " border");
            }
            int buildable = layout.buildableCells().size();
            check(buildable > 600, layout + " has enough building space: " + buildable);
            int[] spawn = layout.playerSpawn();
            check(layout.isBuildable(spawn[0], spawn[1]), layout + " player spawn on grass");
            check(layout.path().length() > 150, layout + " path length " + layout.path().length());
            for (int[] p : layout.startPillars()) {
                check(!layout.isBuildable(p[0], p[1]), layout + " pillar not buildable");
            }
            int[][] wps = layout.waypoints();
            for (int i = 0; i + 1 < wps.length; i++) {
                check(wps[i][0] == wps[i + 1][0] || wps[i][1] == wps[i + 1][1], layout + " axis aligned segments");
            }
        }
    }

    private static void pathSmoothness() {
        for (ArenaLayout layout : ArenaLayout.values()) {
            PathLine path = layout.path();
            double[] a = new double[4];
            double[] b = new double[4];
            for (double lateral : new double[]{-0.8, 0, 0.8}) {
                path.sample(0, lateral, a);
                for (double d = 0.05; d <= path.length(); d += 0.05) {
                    path.sample(d, lateral, b);
                    double jump = Math.hypot(b[0] - a[0], b[1] - a[1]);
                    check(jump < 0.2, layout + " smooth path at " + d + " lateral " + lateral + " jump " + jump);
                    double[] t = a;
                    a = b;
                    b = t;
                }
            }
            path.sample(path.length(), 0, a);
            int[] end = layout.waypoints()[layout.waypoints().length - 1];
            check(Math.abs(a[0] - (end[0] + 0.5)) < 1e-6 && Math.abs(a[1] - (end[1] + 0.5)) < 1e-6,
                    layout + " path ends at last waypoint");
        }
    }

    private static void waveGenerator() {
        WaveGenerator g1 = new WaveGenerator(42, Difficulty.NORMAL, 50);
        WaveGenerator g2 = new WaveGenerator(42, Difficulty.NORMAL, 50);
        for (int n = 1; n <= 60; n++) {
            WaveDef a = g1.wave(n);
            WaveDef b = g2.wave(n);
            check(a.equals(b), "deterministic wave " + n);
            check(a.count() > 0 && a.spec().hp() > 0, "wave " + n + " has creeps");
        }
        for (int n = 10; n <= 40; n += 10) {
            check(g1.kindOf(n) == CreepKind.BOSS, "boss at " + n);
        }
        check(g1.kindOf(50) == CreepKind.FINAL_BOSS, "final boss at 50");
        check(g1.wave(50).element() == null, "final boss is neutral");
        for (int block = 0; block < 8; block++) {
            Set<Element> seen = EnumSet.noneOf(Element.class);
            for (int i = 1; i <= 6; i++) {
                seen.add(g1.elementOf(block * 6 + i));
            }
            check(seen.size() == 6, "each block of six waves has all elements");
            if (block > 0) {
                check(g1.elementOf(block * 6) != g1.elementOf(block * 6 + 1), "no element twice in a row");
            }
        }
        int essences = 0;
        for (int n = 1; n <= 50; n++) {
            if (WaveGenerator.grantsEssence(n)) {
                essences++;
            }
        }
        check(essences == 11, "11 essences during the game");
        check(g1.abilitiesOf(5, CreepKind.NORMAL).isEmpty(), "no abilities early");
        check(new WaveGenerator(1, Difficulty.HARD, 50).wave(20).spec().hp()
                > new WaveGenerator(1, Difficulty.EASY, 50).wave(20).spec().hp(), "difficulty scales hp");
        for (int n = 1; n < 60; n++) {
            check(WaveGenerator.baseHp(n + 1) > WaveGenerator.baseHp(n), "hp grows");
        }
    }

    private static int[] buildableNextToPath(ArenaLayout layout, int skip) {
        int found = 0;
        for (int[] cell : layout.buildableCells()) {
            int x = cell[0];
            int z = cell[1];
            if (layout.isPath(x + 1, z) || layout.isPath(x - 1, z) || layout.isPath(x, z + 1) || layout.isPath(x, z - 1)) {
                if (found++ == skip) {
                    return cell;
                }
            }
        }
        throw new IllegalStateException("no cell next to path");
    }

    private static void economy() {
        Game g = new Game(GameConfig.of(Difficulty.NORMAL, ArenaLayout.SERPENTINE, 1));
        check(g.gold() == Difficulty.NORMAL.startGold(), "start gold");
        check(g.lives() == Difficulty.NORMAL.lives(), "start lives");
        check(g.essences() == 1, "one essence at start");
        int[] wp = ArenaLayout.SERPENTINE.waypoints()[1];
        check(g.canBuild(Towers.ARROW, wp[0], wp[1]) == TdResult.NOT_BUILDABLE, "cannot build on path");
        check(g.canBuild(Towers.FIRE_TOWER, 10, 10) == TdResult.LOCKED, "fire tower locked");
        int[] cell = buildableNextToPath(ArenaLayout.SERPENTINE, 0);
        Tower t = g.build(Towers.ARROW, cell[0], cell[1]);
        check(t != null, "arrow tower built");
        check(g.gold() == Difficulty.NORMAL.startGold() - 40, "gold paid");
        check(g.canBuild(Towers.ARROW, cell[0], cell[1]) == TdResult.OCCUPIED, "cell occupied");
        check(g.upgrade(t) == TdResult.OK && t.tier() == 2, "upgrade to tier 2");
        check(g.gold() == Difficulty.NORMAL.startGold() - 130, "upgrade paid");
        check(g.sellValue(t) == 130, "full refund in the same build phase");
        check(g.sell(t) == 130 && g.gold() == Difficulty.NORMAL.startGold(), "sold");
        check(g.towerAt(cell[0], cell[1]) == null, "tower removed");
        check(g.canBuild(Towers.CANNON, cell[0], cell[1]) == TdResult.OK, "cell free again");
        g.addGold(-g.gold());
        check(g.canBuild(Towers.ARROW, cell[0], cell[1]) == TdResult.NOT_ENOUGH_GOLD, "no gold");
    }

    private static void elementsAndGuardian() {
        Game g = new Game(GameConfig.of(Difficulty.NORMAL, ArenaLayout.SERPENTINE, 2));
        List<Creep> guardians = new ArrayList<>();
        g.setListener(new GameListener() {
            @Override
            public void elementBought(Element element, int level, Creep guardian) {
                guardians.add(guardian);
            }
        });
        check(g.buyElement(Element.FIRE) == TdResult.OK, "buy fire");
        check(g.level(Element.FIRE) == 1 && g.essences() == 0, "fire level 1");
        check(guardians.size() == 1 && guardians.get(0) != null, "guardian spawned");
        check(guardians.get(0).spec().kind() == CreepKind.ELEMENTAL, "guardian kind");
        check(g.buyElement(Element.WATER) == TdResult.NO_ESSENCE, "no essence left");
        g.addEssences(5);
        g.buyElement(Element.FIRE);
        g.buyElement(Element.FIRE);
        check(g.buyElement(Element.FIRE) == TdResult.ELEMENT_MAXED, "max level 3");
        g.addGold(10000);
        int[] cell = buildableNextToPath(ArenaLayout.SERPENTINE, 3);
        Tower fire = g.build(Towers.FIRE_TOWER, cell[0], cell[1]);
        check(fire != null, "fire tower built after unlocking");
        check(g.upgrade(fire) == TdResult.OK && g.upgrade(fire) == TdResult.OK, "fire tower upgraded twice");
        check(g.upgrade(fire) == TdResult.MAX_TIER, "fire tower maxed");
        check(g.canBuild(Towers.STEAM, cell[0] + 1, cell[1]) == TdResult.LOCKED
                || g.canBuild(Towers.STEAM, cell[0] + 1, cell[1]) == TdResult.NOT_BUILDABLE, "steam needs water");
    }

    private static void combatClearsFirstWave() {
        Game g = new Game(GameConfig.of(Difficulty.NORMAL, ArenaLayout.SERPENTINE, 3));
        int[] cleared = {0};
        g.setListener(new GameListener() {
            @Override
            public void waveCleared(int wave, int bonus, int interest, int towerGold) {
                cleared[0] = wave;
            }
        });
        for (int i = 0; i < 4; i++) {
            int[] cell = buildableNextToPath(ArenaLayout.SERPENTINE, i * 7);
            check(g.build(Towers.ARROW, cell[0], cell[1]) != null, "arrow " + i);
        }
        int goldBefore = g.gold();
        check(g.callNextWave() == TdResult.OK, "call wave 1");
        check(g.lastEarlyBonus() > 0, "early call bonus");
        for (int i = 0; i < 20 * 120 && cleared[0] == 0; i++) {
            g.tick();
        }
        check(cleared[0] == 1, "wave 1 cleared");
        check(g.kills() >= 1, "creeps killed");
        check(g.gold() > goldBefore, "gold earned");
        check(g.phase() == Game.Phase.BUILD, "back in build phase");
        check(g.currentWave() == 1 && g.nextWave() == 2, "wave counter");
    }

    private static void undeadAndSplit() {
        Game g = new Game(new GameConfig(Difficulty.NORMAL, ArenaLayout.SERPENTINE, 4, 50, false));
        int[] fell = {0};
        int[] rose = {0};
        int[] died = {0};
        g.setListener(new GameListener() {
            @Override
            public void creepFell(Creep creep) {
                fell[0]++;
            }

            @Override
            public void creepRose(Creep creep) {
                rose[0]++;
            }

            @Override
            public void creepDied(Creep creep, Tower killer) {
                died[0]++;
            }
        });
        g.addGold(100000);
        for (int i = 0; i < 6; i++) {
            int[] cell = buildableNextToPath(ArenaLayout.SERPENTINE, i * 3);
            Tower t = g.build(Towers.ARROW, cell[0], cell[1]);
            g.upgrade(t);
            g.upgrade(t);
        }
        CreepSpec undead = new CreepSpec(0, Element.FIRE, CreepKind.NORMAL, EnumSet.of(Ability.UNDEAD, Ability.SPLIT),
                200, 2.0, 5, 1, "minecraft:zombie", 1f, 0, false);
        g.spawnExtra(undead);
        for (int i = 0; i < 20 * 120 && died[0] < 3; i++) {
            g.tick();
        }
        check(fell[0] >= 1 && rose[0] >= 1, "undead fell and rose: " + fell[0] + "/" + rose[0]);
        check(died[0] == 3, "undead splitter died and split into two children which died: " + died[0]);
    }

    private static void snapshotRoundTrip() {
        Game g = new Game(GameConfig.of(Difficulty.HARD, ArenaLayout.SPIRAL, 5));
        g.addGold(5000);
        g.buyElement(Element.NATURE);
        int[] cell = buildableNextToPath(ArenaLayout.SPIRAL, 2);
        Tower t = g.build(Towers.NATURE_TOWER, cell[0], cell[1]);
        g.setTargetMode(t, TargetMode.STRONGEST);
        g.callNextWave();
        for (int i = 0; i < 200; i++) {
            g.tick();
        }
        Game.Snapshot s = g.snapshot();
        Game r = Game.restore(s);
        check(r.gold() == g.gold() && r.lives() == g.lives() && r.essences() == g.essences(), "economy restored");
        check(r.level(Element.NATURE) == 1, "levels restored");
        check(r.nextWave() == 1, "running wave is replayed: " + r.nextWave());
        check(r.phase() == Game.Phase.BUILD, "restored into build phase");
        Tower rt = r.towerAt(cell[0], cell[1]);
        check(rt != null && rt.type() == Towers.NATURE_TOWER && rt.mode() == TargetMode.STRONGEST, "tower restored");
        check(r.config().equals(g.config()), "config restored");
    }

    private static void defeat() {
        Game g = new Game(GameConfig.of(Difficulty.HARD, ArenaLayout.ZIGZAG, 6));
        boolean[] over = {false};
        g.setListener(new GameListener() {
            @Override
            public void gameOver(boolean victory) {
                over[0] = !victory;
            }
        });
        for (int i = 0; i < 20 * 60 * 30 && !g.isOver(); i++) {
            if (g.phase() == Game.Phase.BUILD) {
                g.callNextWave();
            }
            g.tick();
        }
        check(g.phase() == Game.Phase.DEFEAT && over[0], "game without towers is lost");
        check(g.lives() == 0, "no lives left");
    }
}
