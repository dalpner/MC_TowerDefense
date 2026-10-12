package io.github.dalpner.mctd.game;

import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.Towers;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.Tower;
import io.github.dalpner.mctd.world.ArenaDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Automated end-to-end check for CI, enabled with {@code -Dmctd.smoketest=true}: on a dedicated server it
 * builds an arena, places towers, plays the first waves without any player, verifies the result and stops
 * the server. The outcome is logged as {@code [SmokeTest] PASSED} or {@code [SmokeTest] FAILED}.
 */
public final class SmokeTest {
    private static final int TICKS = 20 * 60 * 4;
    private static final TowerType[] TYPES = {Towers.ARROW, Towers.CANNON, Towers.FIRE_TOWER, Towers.WATER_TOWER,
            Towers.NATURE_TOWER, Towers.STEAM, Towers.FORGE, Towers.SPRING, Towers.GEYSER};

    private static Session session;
    private static int ticks;
    private static int maxBodies;
    private static boolean done;

    private SmokeTest() {
    }

    public static void register() {
        if (!Boolean.getBoolean("mctd.smoketest")) {
            return;
        }
        MctdMod.LOGGER.info("[SmokeTest] enabled");
        ServerLifecycleEvents.SERVER_STARTED.register(SmokeTest::start);
        ServerTickEvents.END_SERVER_TICK.register(SmokeTest::tick);
    }

    private static void start(MinecraftServer server) {
        ServerWorld arena = ArenaDimension.get(server);
        if (arena == null) {
            finish(server, "the arena dimension is missing");
            return;
        }
        session = SessionManager.startHeadless(server, arena, Difficulty.EASY, ArenaLayout.SERPENTINE, 4242L);
        Game game = session.game();
        game.addGold(50000);
        game.addEssences(8);
        for (Element element : new Element[]{Element.FIRE, Element.WATER, Element.NATURE}) {
            game.buyElement(element);
            game.buyElement(element);
        }
        ArenaLayout layout = session.layout();
        int built = 0;
        int candidates = 0;
        for (int[] cell : layout.buildableCells()) {
            if (built >= 24) {
                break;
            }
            int x = cell[0];
            int z = cell[1];
            boolean nextToPath = layout.isPath(x + 1, z) || layout.isPath(x - 1, z)
                    || layout.isPath(x, z + 1) || layout.isPath(x, z - 1);
            if (!nextToPath || candidates++ % 3 != 0) {
                continue;
            }
            Tower tower = game.build(TYPES[built % TYPES.length], x, z);
            if (tower != null) {
                built++;
                if (built % 2 == 0) {
                    game.upgrade(tower);
                }
            }
        }
        session.placeTowerBlocks();
        game.callNextWave();
        server.getTickManager().startSprint(TICKS + 200);
        MctdMod.LOGGER.info("[SmokeTest] arena built, {} towers placed, first wave started", built);
        if (built < 10) {
            finish(server, "only " + built + " towers could be built");
        }
    }

    private static void tick(MinecraftServer server) {
        if (session == null || done) {
            return;
        }
        ticks++;
        Game game = session.game();
        maxBodies = Math.max(maxBodies, session.bodyCount());
        if (!game.isOver() && game.phase() == Game.Phase.BUILD) {
            game.callNextWave();
        }
        if (ticks >= TICKS || game.isOver()) {
            finish(server, verify(server));
        }
    }

    private static String verify(MinecraftServer server) {
        Game game = session.game();
        if (SessionManager.errorCount() > 0) {
            return SessionManager.errorCount() + " errors while ticking the game";
        }
        if (maxBodies == 0) {
            return "no creep entities were spawned";
        }
        if (game.kills() == 0) {
            return "the towers did not kill a single creep";
        }
        if (game.currentWave() < 2) {
            return "the waves did not progress (wave " + game.currentWave() + ")";
        }
        for (Tower tower : game.towers()) {
            if (!(session.world().getBlockState(session.towerPos(tower)).getBlock() instanceof TowerBlock)) {
                return "tower block missing at " + session.towerPos(tower);
            }
        }
        int[] waypoint = session.layout().waypoints()[1];
        if (!session.world().getBlockState(session.origin().add(waypoint[0], 0, waypoint[1])).isOf(Blocks.DIRT_PATH)) {
            return "the arena path was not built";
        }
        Session copy = SessionCodec.read(server, session.world(), SessionCodec.write(session));
        if (copy == null || copy.game().gold() != game.gold() || copy.game().towers().size() != game.towers().size()) {
            return "saving and loading the game failed";
        }
        return null;
    }

    private static void finish(MinecraftServer server, String failure) {
        done = true;
        if (failure == null) {
            Game game = session.game();
            MctdMod.LOGGER.info("[SmokeTest] PASSED: wave {}, {} kills, {} leaks, {} towers, up to {} creeps at once",
                    game.currentWave(), game.kills(), game.leaks(), game.towers().size(), maxBodies);
        } else {
            MctdMod.LOGGER.error("[SmokeTest] FAILED: {}", failure);
        }
        server.stop(false);
    }
}
