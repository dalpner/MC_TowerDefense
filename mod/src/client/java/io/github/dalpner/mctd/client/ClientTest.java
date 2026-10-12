package io.github.dalpner.mctd.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.game.SessionManager;
import io.github.dalpner.mctd.registry.ModBlocks;
import io.github.dalpner.mctd.registry.ModItems;
import io.github.dalpner.mctd.world.ArenaDimension;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Language;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Automated client check for CI, enabled with {@code -Dmctd.clienttest=true}. It plays like a person would:
 * creates a singleplayer world through the normal screens, opens the lobby with {@code /td lobby}, starts a
 * game, builds towers with the builder's wand, opens the tower and main menus, calls a wave, saves and reloads
 * the world and quits the game. On the way it checks models, textures and translations and saves screenshots
 * to {@code screenshots/}. The outcome is logged as {@code [ClientTest] PASSED} or {@code [ClientTest] FAILED}.
 */
public final class ClientTest {
    private static final Duration SHORT = Duration.ofSeconds(30);
    private static final Duration LONG = Duration.ofMinutes(5);
    /** Buttons that confirm a warning shown while a world is created or loaded. */
    private static final String[] PROCEED_KEYS = {"selectWorld.backupJoinSkipButton", "gui.proceed", "gui.yes", "gui.continue"};
    private static final int TOWERS = 4;

    private static final List<String> PROBLEMS = Collections.synchronizedList(new ArrayList<>());
    private static int screenshots;
    private static int maxCreeps;

    private ClientTest() {
    }

    public static void register() {
        if (!Boolean.getBoolean("mctd.clienttest")) {
            return;
        }
        MctdMod.LOGGER.info("[ClientTest] enabled");
        Thread thread = new Thread(ClientTest::run, "MCTD client test");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run() {
        String summary = null;
        try {
            summary = play();
        } catch (Throwable t) {
            MctdMod.LOGGER.error("[ClientTest] aborted", t);
            PROBLEMS.add(0, "aborted: " + t);
        }
        boolean passed = summary != null && PROBLEMS.isEmpty();
        if (passed) {
            MctdMod.LOGGER.info("[ClientTest] PASSED: {}", summary);
        } else {
            MctdMod.LOGGER.error("[ClientTest] FAILED: {}", String.join("; ", PROBLEMS));
        }
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(client::scheduleStop);
        sleep(Duration.ofMinutes(2));
        MctdMod.LOGGER.error("[ClientTest] the game did not stop, halting");
        Runtime.getRuntime().halt(passed ? 0 : 1);
    }

    private static String play() throws Exception {
        waitFor("the game to finish loading", c -> c.getOverlay() == null, LONG);
        String assets = onClient(c -> checkModels(c) + ", " + checkTranslations(c));
        onClient(c -> {
            // keep software rendering in CI fast enough
            c.options.onboardAccessibility = false;
            c.options.pauseOnLostFocus = false;
            c.options.getViewDistance().setValue(6);
            c.options.getSimulationDistance().setValue(6);
            c.options.write();
            c.setScreen(new TitleScreen(false));
            return null;
        });
        screenshot("title_screen");

        // a new singleplayer world, created through the normal screens
        onClient(c -> {
            CreateWorldScreen.create(c, c.currentScreen);
            return null;
        });
        waitFor("the create world screen", c -> c.currentScreen instanceof CreateWorldScreen, SHORT);
        press("selectWorld.create");
        enterWorld("creating the world");
        screenshot("new_world");
        UUID me = onClient(c -> c.player.getUuid());
        IntegratedServer server = onClient(MinecraftClient::getServer);
        MctdMod.LOGGER.info("[ClientTest] world lifecycle {}, registries {}", onServer(server,
                () -> server.getSaveProperties().getLifecycle()), onServer(server,
                () -> server.getRegistryManager().getRegistryLifecycle()));

        // the lobby menu starts an easy game on the first map
        command("td lobby");
        waitForMenu(Text.translatable("mctd.menu.lobby"));
        screenshot("lobby_menu");
        clickSlot(12);
        waitFor("the arena", c -> inArena(c) && ClientState.hud().active() && c.currentScreen == null, Duration.ofMinutes(1));
        screenshot("arena");

        // towers are built with the builder's wand next to the start of the path
        List<BlockPos> spots = onServer(server, () -> buildSpots(SessionManager.byMember(me)));
        if (spots.size() < TOWERS) {
            throw new IllegalStateException("only " + spots.size() + " build spots found");
        }
        // the arena decorates build spots with flowers; they must not get in the way
        onServer(server, () -> SessionManager.byMember(me).world().setBlockState(spots.get(0).up(),
                Blocks.POPPY.getDefaultState(), Block.NOTIFY_ALL));
        selectWand();
        for (int i = 0; i < TOWERS; i++) {
            BlockPos floor = spots.get(i);
            moveTo(server, me, Vec3d.ofBottomCenter(floor.up(3)), 0f, 90f);
            useWandOn(floor, floor.up());
            waitForMenu(Text.translatable("mctd.menu.build"));
            if (i == 0) {
                screenshot("build_menu");
            }
            clickSlot(i % 2); // arrow and cannon tower
            BlockPos tower = floor.up();
            waitFor("the tower at " + tower, c -> c.currentScreen == null
                    && c.world.getBlockState(tower).getBlock() instanceof TowerBlock, SHORT);
        }
        BlockPos firstTower = spots.get(0).up();
        Vec3d middle = Vec3d.ofCenter(firstTower);
        Vec3d center = onServer(server, () -> SessionManager.byMember(me).center());
        Vec3d side = middle.add(new Vec3d(center.x - middle.x, 0, center.z - middle.z).normalize().multiply(5)).add(0, 1.5, 0);
        moveTo(server, me, side, (float) Math.toDegrees(Math.atan2(side.x - middle.x, middle.z - side.z)),
                (float) Math.toDegrees(Math.atan2(side.y + 1.62 - middle.y, 5)));
        screenshot("towers");
        moveTo(server, me, Vec3d.ofBottomCenter(firstTower.up(2)), 0f, 90f);
        waitFor("the tower preview", c -> TowerPreview.target() != null, SHORT);
        screenshot("tower_preview");
        useWandOn(firstTower, firstTower.up());
        waitFor("the tower menu", c -> c.currentScreen instanceof GenericContainerScreen, SHORT);
        screenshot("tower_menu");
        closeMenu();
        onClient(c -> c.interactionManager.interactItem(c.player, Hand.MAIN_HAND));
        waitForMenu(Text.translatable("mctd.menu.main"));
        screenshot("main_menu");
        closeMenu();

        // the first wave, watched from above the arena
        command("td next");
        Vec3d overview = onServer(server, () -> {
            Session session = SessionManager.byMember(me);
            double size = session.layout().size();
            return new Vec3d(session.origin().getX() + size / 2, session.origin().getY() + size * 0.55,
                    session.origin().getZ() + size * 1.05);
        });
        moveTo(server, me, overview, 180f, 42f);
        waitFor("creeps", c -> {
            maxCreeps = Math.max(maxCreeps, countCreeps(c));
            return maxCreeps > 0;
        }, Duration.ofMinutes(1));
        sleep(Duration.ofSeconds(4));
        screenshot("wave");
        long end = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (onServer(server, () -> SessionManager.byMember(me).game().kills()) == 0) {
            maxCreeps = Math.max(maxCreeps, onClient(ClientTest::countCreeps));
            if (System.nanoTime() > end) {
                throw new IllegalStateException("the towers did not kill a single creep");
            }
            sleep(Duration.ofMillis(500));
        }
        screenshot("fight");
        int[] stats = onServer(server, () -> {
            Game game = SessionManager.byMember(me).game();
            return new int[]{game.currentWave(), game.kills(), SessionManager.errorCount()};
        });
        if (stats[2] > 0) {
            problem(stats[2] + " errors while ticking the game");
        }
        if (FxRenderer.entriesPlayed() == 0) {
            problem("no tower effects arrived");
        }

        // save, quit to the title screen and load the world again
        onClient(c -> {
            c.world.disconnect();
            c.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
            c.setScreen(new TitleScreen(false));
            return null;
        });
        waitFor("the title screen", c -> c.world == null && c.getServer() == null && c.currentScreen instanceof TitleScreen, LONG);
        String level = onClient(ClientTest::newestSave);
        onClient(c -> {
            c.createIntegratedServerLoader().start(level, () -> c.setScreen(new TitleScreen(false)));
            return null;
        });
        enterWorld("loading the world");
        waitFor("the game to resume", c -> inArena(c) && ClientState.hud().active(), Duration.ofMinutes(1));
        for (BlockPos floor : spots.subList(0, TOWERS)) {
            BlockPos tower = floor.up();
            waitFor("the tower at " + tower + " after loading",
                    c -> c.world.getBlockState(tower).getBlock() instanceof TowerBlock, SHORT);
        }
        screenshot("reloaded");
        // creeps are not saved, so the interrupted wave is played again; towers and kills stay
        IntegratedServer reloaded = onClient(MinecraftClient::getServer);
        int[] restored = onServer(reloaded, () -> {
            Session session = SessionManager.byMember(me);
            return session == null ? new int[]{-1, -1} : new int[]{session.game().towers().size(), session.game().kills()};
        });
        if (restored[0] != TOWERS || restored[1] < stats[1]) {
            problem("the game was not restored (" + restored[0] + " towers, " + restored[1] + " kills)");
        }

        // leave the game for good
        command("td quit");
        waitFor("the way home", c -> c.world != null && !inArena(c) && !ClientState.hud().active() && c.currentScreen == null,
                Duration.ofMinutes(1));
        screenshot("back_home");
        return assets + ", wave " + stats[0] + ", " + stats[1] + " kills, up to " + maxCreeps + " creeps visible, "
                + FxRenderer.entriesPlayed() + " effects, " + screenshots + " screenshots";
    }

    // ================================================================== checks

    private static String checkModels(MinecraftClient c) {
        BakedModelManager models = c.getBakedModelManager();
        BakedModel missing = models.getMissingModel();
        Random random = Random.create(42);
        int states = 0;
        List<Item> items = new ArrayList<>();
        for (TowerBlock block : ModBlocks.towers()) {
            for (BlockState state : block.getStateManager().getStates()) {
                checkModel(models.getBlockModels().getModel(state), missing, state, random, state.toString());
                states++;
            }
            items.add(block.asItem());
        }
        items.add(ModItems.BUILDER_WAND);
        for (Item item : items) {
            checkModel(c.getItemRenderer().getModels().getModel(new ItemStack(item)), missing, null, random,
                    "item " + Registries.ITEM.getId(item));
        }
        return "models of " + states + " block states and " + items.size() + " items";
    }

    private static void checkModel(BakedModel model, BakedModel missing, BlockState state, Random random, String what) {
        if (model == null || model == missing) {
            problem(what + " has no model");
            return;
        }
        List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, random));
        for (Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(state, direction, random));
        }
        if (quads.isEmpty()) {
            problem(what + " has an empty model");
        }
        boolean missingTexture = isMissing(model.getParticleSprite());
        for (BakedQuad quad : quads) {
            missingTexture |= isMissing(quad.getSprite());
        }
        if (missingTexture) {
            problem(what + " uses a missing texture");
        }
    }

    private static boolean isMissing(Sprite sprite) {
        return sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId());
    }

    /** Every text of the mod is translated into the language the game runs in. */
    private static String checkTranslations(MinecraftClient c) {
        String language = c.options.language;
        Optional<Resource> file = c.getResourceManager().getResource(MctdMod.id("lang/" + language + ".json"));
        if (file.isEmpty()) {
            problem("no translations for " + language);
            return "no translations";
        }
        JsonObject texts;
        try (Reader reader = file.get().getReader()) {
            texts = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> keys = new ArrayList<>(texts.keySet());
        for (TowerBlock block : ModBlocks.towers()) {
            keys.add(block.getTranslationKey());
        }
        keys.add(ModItems.BUILDER_WAND.getTranslationKey());
        List<String> wrong = new ArrayList<>();
        for (String key : keys) {
            // the game turns %d and %f into %s when it loads a language file
            if (!texts.has(key) || !Language.getInstance().get(key)
                    .equals(texts.get(key).getAsString().replaceAll("%(\\d+\\$)?[\\d.]*[df]", "%$1s"))) {
                wrong.add(key);
            }
        }
        if (!wrong.isEmpty()) {
            problem(wrong.size() + " texts are not translated into " + language + ", e.g. "
                    + wrong.subList(0, Math.min(5, wrong.size())));
        }
        return texts.size() + " texts in " + language;
    }

    // ================================================================== actions

    private static void press(String key) throws Exception {
        String label = Text.translatable(key).getString();
        waitFor("the button " + label, c -> {
            PressableWidget button = button(c.currentScreen, label);
            if (button == null) {
                return false;
            }
            button.onPress();
            return true;
        }, SHORT);
    }

    private static PressableWidget button(Screen screen, String label) {
        if (screen == null) {
            return null;
        }
        for (Element element : screen.children()) {
            if (element instanceof PressableWidget button && button.active && button.getMessage().getString().equals(label)) {
                return button;
            }
        }
        return null;
    }

    /** Waits until the player is in the world; warnings on the way are confirmed and reported. */
    private static void enterWorld(String what) throws Exception {
        long end = System.nanoTime() + LONG.toNanos();
        while (true) {
            String state = onClient(c -> {
                if (c.world != null && c.player != null && c.currentScreen == null) {
                    return "";
                }
                for (String key : PROCEED_KEYS) {
                    PressableWidget button = button(c.currentScreen, Text.translatable(key).getString());
                    if (button != null) {
                        String warning = c.currentScreen.getClass().getSimpleName() + " '" + c.currentScreen.getTitle().getString() + "'";
                        button.onPress();
                        return warning;
                    }
                }
                return null;
            });
            if ("".equals(state)) {
                break;
            }
            if (state != null) {
                problem("a warning appeared while " + what + ": " + state);
            }
            if (System.nanoTime() > end) {
                throw new IllegalStateException("timed out " + what + " (screen: " + screenName() + ")");
            }
            sleep(Duration.ofMillis(250));
        }
        sleep(Duration.ofSeconds(3));
    }

    private static void command(String command) throws Exception {
        onClient(c -> {
            c.getNetworkHandler().sendChatCommand(command);
            return null;
        });
    }

    private static void waitForMenu(Text title) throws Exception {
        String expected = title.getString();
        waitFor("the menu " + expected, c -> c.currentScreen instanceof GenericContainerScreen screen
                && screen.getTitle().getString().equals(expected), SHORT);
        sleep(Duration.ofMillis(500));
    }

    private static void clickSlot(int slot) throws Exception {
        onClient(c -> {
            if (!(c.currentScreen instanceof GenericContainerScreen screen)) {
                throw new IllegalStateException("no menu is open");
            }
            ItemStack stack = screen.getScreenHandler().getSlot(slot).getStack();
            if (stack.isEmpty() || stack.isOf(Items.GRAY_STAINED_GLASS_PANE)) {
                throw new IllegalStateException("slot " + slot + " of " + screen.getTitle().getString() + " is empty");
            }
            c.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, c.player);
            return null;
        });
    }

    private static void closeMenu() throws Exception {
        onClient(c -> {
            c.player.closeHandledScreen();
            return null;
        });
        waitFor("the menu to close", c -> c.currentScreen == null, SHORT);
    }

    private static void selectWand() throws Exception {
        waitFor("the builder's wand", c -> {
            for (int i = 0; i < 9; i++) {
                if (c.player.getInventory().getStack(i).isOf(ModItems.BUILDER_WAND)) {
                    c.player.getInventory().selectedSlot = i;
                    return true;
                }
            }
            return false;
        }, SHORT);
    }

    /** Right-clicks the top of a block with the wand. */
    private static void useWandOn(BlockPos pos, BlockPos above) throws Exception {
        onClient(c -> c.interactionManager.interactBlock(c.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofBottomCenter(above), Direction.UP, pos, false)));
    }

    /** Lets the player fly to a position; the server ignores clicks until it saw the client arrive. */
    private static void moveTo(IntegratedServer server, UUID me, Vec3d pos, float yaw, float pitch) throws Exception {
        onServer(server, () -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(me);
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
            player.teleport(player.getServerWorld(), pos.x, pos.y, pos.z, yaw, pitch);
            return null;
        });
        waitFor("the player to arrive at " + pos, c -> c.player.getPos().squaredDistanceTo(pos) < 0.25, SHORT);
        sleep(Duration.ofMillis(500));
    }

    // ================================================================== queries

    /** Buildable cells next to the path, near where the creeps enter the arena. */
    private static List<BlockPos> buildSpots(Session session) {
        ArenaLayout layout = session.layout();
        int[] entry = layout.waypoints()[0];
        List<int[]> cells = new ArrayList<>();
        for (int[] cell : layout.buildableCells()) {
            int x = cell[0];
            int z = cell[1];
            if (layout.isPath(x + 1, z) || layout.isPath(x - 1, z) || layout.isPath(x, z + 1) || layout.isPath(x, z - 1)) {
                cells.add(cell);
            }
        }
        cells.sort(Comparator.comparingDouble(cell -> Math.hypot(cell[0] - entry[0], cell[1] - entry[1])));
        List<BlockPos> spots = new ArrayList<>();
        for (int[] cell : cells) {
            BlockPos pos = session.origin().add(cell[0], 0, cell[1]);
            if (spots.size() < TOWERS && spots.stream().allMatch(p -> p.getManhattanDistance(pos) >= 3)) {
                spots.add(pos);
            }
        }
        return spots;
    }

    private static boolean inArena(MinecraftClient c) {
        return c.world != null && c.world.getRegistryKey().equals(ArenaDimension.WORLD);
    }

    private static int countCreeps(MinecraftClient c) {
        int count = 0;
        if (c.world != null) {
            for (Entity entity : c.world.getEntities()) {
                if (entity instanceof MobEntity) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String newestSave(MinecraftClient c) {
        try (Stream<Path> saves = Files.list(c.getLevelStorage().getSavesDirectory())) {
            return saves.filter(Files::isDirectory)
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                    .map(path -> path.getFileName().toString())
                    .orElseThrow(() -> new IllegalStateException("no saved world"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String screenName() {
        try {
            return onClient(c -> c.currentScreen == null ? "none"
                    : c.currentScreen.getClass().getSimpleName() + " '" + c.currentScreen.getTitle().getString() + "'");
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ================================================================== helpers

    private static void problem(String problem) {
        MctdMod.LOGGER.error("[ClientTest] {}", problem);
        if (PROBLEMS.size() < 20) {
            PROBLEMS.add(problem);
        }
    }

    private static void screenshot(String name) throws Exception {
        sleep(Duration.ofSeconds(1));
        String file = String.format("%02d_%s.png", ++screenshots, name);
        onClient(c -> {
            ScreenshotRecorder.saveScreenshot(c.runDirectory, file, c.getFramebuffer(), message -> {
            });
            return null;
        });
    }

    private static void waitFor(String what, Predicate<MinecraftClient> condition, Duration timeout) throws Exception {
        long end = System.nanoTime() + timeout.toNanos();
        while (!onClient(condition::test)) {
            if (System.nanoTime() > end) {
                throw new IllegalStateException("timed out waiting for " + what + " (screen: " + screenName() + ")");
            }
            sleep(Duration.ofMillis(250));
        }
    }

    private static <T> T onClient(Function<MinecraftClient, T> action) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.submit(() -> action.apply(client)).get(5, TimeUnit.MINUTES);
    }

    private static <T> T onServer(IntegratedServer server, Supplier<T> action) throws Exception {
        return server.submit(action).get(1, TimeUnit.MINUTES);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
