package io.github.dalpner.mctd.game;

import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.core.WaveGenerator;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.GameConfig;
import io.github.dalpner.mctd.world.ArenaBuilder;
import io.github.dalpner.mctd.world.ArenaDimension;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Keeps track of all games on the server and wires them to Minecraft's events. */
public final class SessionManager {
    private static final List<Session> SESSIONS = new ArrayList<>();
    private static final Set<UUID> BODIES = new HashSet<>();
    private static final List<Entity> TO_DISCARD = new ArrayList<>();
    private static MinecraftServer server;
    private static SessionState state;

    private SessionManager() {
    }

    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(SessionManager::onStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(SessionManager::onStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> onStopped());
        ServerTickEvents.END_SERVER_TICK.register(s -> tick());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> onDisconnect(handler.getPlayer()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(SessionManager::onChangeWorld);
        ServerEntityEvents.ENTITY_LOAD.register(SessionManager::onEntityLoad);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, blockState, blockEntity)
                -> !ArenaDimension.is(world) || player.isCreative());
    }

    // ================================================================== lifecycle

    private static void onStarted(MinecraftServer srv) {
        server = srv;
        SESSIONS.clear();
        BODIES.clear();
        cleanupTeams();
        state = srv.getOverworld().getPersistentStateManager().getOrCreate(SessionState.TYPE, SessionState.NAME);
        ServerWorld arena = ArenaDimension.get(srv);
        if (arena == null) {
            MctdMod.LOGGER.error("The arena dimension {} is missing; tower defense games cannot run",
                    ArenaDimension.WORLD.getValue());
            return;
        }
        NbtList saved = state.loaded();
        for (int i = 0; i < saved.size(); i++) {
            Session session = SessionCodec.read(srv, arena, saved.getCompound(i));
            if (session != null && byMember(session.owner()) == null && bySlot(session.slot()) == null) {
                ArenaBuilder.loadChunks(arena, session.origin(), session.layout(), true);
                session.placeTowerBlocks();
                SESSIONS.add(session);
            }
        }
        if (!SESSIONS.isEmpty()) {
            MctdMod.LOGGER.info("Restored {} tower defense game(s)", SESSIONS.size());
        }
    }

    private static void onStopping(MinecraftServer srv) {
        for (Session session : SESSIONS) {
            session.discardBodies();
        }
        if (state != null) {
            state.markDirty();
        }
    }

    private static void onStopped() {
        SESSIONS.clear();
        BODIES.clear();
        TO_DISCARD.clear();
        state = null;
        server = null;
    }

    private static void tick() {
        if (!TO_DISCARD.isEmpty()) {
            for (Entity e : TO_DISCARD) {
                if (!e.isRemoved()) {
                    e.discard();
                }
            }
            TO_DISCARD.clear();
        }
        for (Session session : new ArrayList<>(SESSIONS)) {
            try {
                session.tick();
            } catch (RuntimeException e) {
                MctdMod.LOGGER.error("Error in tower defense game {}", session.slot(), e);
            }
        }
        if (state != null && !SESSIONS.isEmpty()) {
            state.markDirty();
        }
    }

    /** Removes the scoreboard entries of creeps left over from an earlier run. */
    private static void cleanupTeams() {
        Scoreboard scoreboard = server.getScoreboard();
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("mctd_")) {
                for (String holder : new ArrayList<>(team.getPlayerList())) {
                    scoreboard.removeScoreHolderFromTeam(holder, team);
                }
            }
        }
    }

    static NbtList writeSessions() {
        NbtList list = new NbtList();
        for (Session session : SESSIONS) {
            list.add(SessionCodec.write(session));
        }
        return list;
    }

    // ================================================================== creep bodies

    static void trackBody(UUID uuid) {
        BODIES.add(uuid);
    }

    static void untrackBody(UUID uuid) {
        BODIES.remove(uuid);
    }

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity.getCommandTags().contains(Session.CREEP_TAG) && !BODIES.contains(entity.getUuid())) {
            TO_DISCARD.add(entity);
        }
    }

    // ================================================================== players

    private static void onJoin(ServerPlayerEntity player) {
        Session session = byMember(player.getUuid());
        if (session != null) {
            if (player.getWorld() == session.world()) {
                session.onArrive(player);
            }
            player.sendMessage(Text.translatable("mctd.msg.rejoin").formatted(Formatting.GREEN), false);
            return;
        }
        if (ArenaDimension.is(player.getWorld())) {
            ReturnPoint home = ReturnPoint.spawn(player.getServer());
            home.teleport(player, player.getServer());
            home.restoreMode(player);
        }
        player.sendMessage(welcome(), false);
    }

    private static void onDisconnect(ServerPlayerEntity player) {
        Session session = byMember(player.getUuid());
        if (session != null) {
            session.onDisconnect(player);
        }
    }

    private static void onChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        Session session = byMember(player.getUuid());
        if (session == null) {
            return;
        }
        if (destination == session.world()) {
            session.onArrive(player);
        } else if (origin == session.world()) {
            session.onDepart(player);
        }
    }

    public static MutableText welcome() {
        MutableText start = Text.translatable("mctd.msg.welcome.start").styled(s -> s.withColor(Formatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/td lobby"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("mctd.msg.welcome.start.hover"))));
        MutableText help = Text.translatable("mctd.msg.welcome.help").styled(s -> s.withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/td help")));
        return Text.translatable("mctd.msg.welcome").formatted(Formatting.GOLD)
                .append(Text.literal(" "))
                .append(start)
                .append(Text.literal(" "))
                .append(help);
    }

    // ================================================================== queries

    public static List<Session> sessions() {
        return Collections.unmodifiableList(SESSIONS);
    }

    public static Session byMember(UUID uuid) {
        for (Session session : SESSIONS) {
            if (session.isMember(uuid)) {
                return session;
            }
        }
        return null;
    }

    public static Session bySlot(int slot) {
        for (Session session : SESSIONS) {
            if (session.slot() == slot) {
                return session;
            }
        }
        return null;
    }

    /** The session whose arena contains the given position of the arena dimension. */
    public static Session at(BlockPos pos) {
        for (Session session : SESSIONS) {
            if (session.contains(pos)) {
                return session;
            }
        }
        return null;
    }

    public static BlockPos originOf(int slot) {
        return new BlockPos(slot * ArenaDimension.SLOT_SPACING, ArenaDimension.FLOOR_Y, 0);
    }

    // ================================================================== actions

    /** Creates a new game for the player and brings them into the arena. */
    public static Session start(ServerPlayerEntity player, Difficulty difficulty, ArenaLayout layout) {
        if (byMember(player.getUuid()) != null) {
            player.sendMessage(Text.translatable("mctd.msg.already_in_game").formatted(Formatting.RED), false);
            return null;
        }
        ServerWorld arena = ArenaDimension.get(player.getServer());
        if (arena == null) {
            player.sendMessage(Text.translatable("mctd.msg.no_dimension").formatted(Formatting.RED), false);
            return null;
        }
        int slot = 0;
        while (bySlot(slot) != null) {
            slot++;
        }
        BlockPos origin = originOf(slot);
        long seed = arena.getRandom().nextLong();
        ArenaBuilder.build(arena, origin, layout, seed);
        Game game = new Game(new GameConfig(difficulty, layout, seed, WaveGenerator.DEFAULT_WAVES, true));
        Session session = new Session(player.getServer(), arena, slot, origin, game, player.getUuid());
        SESSIONS.add(session);
        session.enter(player);
        player.sendMessage(Text.translatable("mctd.msg.started", Text.translatable(difficulty.translationKey()),
                Text.translatable(layout.translationKey())).formatted(Formatting.GREEN), false);
        player.sendMessage(Text.translatable("mctd.msg.tutorial").formatted(Formatting.GRAY), false);
        return session;
    }

    public static boolean join(ServerPlayerEntity player, Session session) {
        if (byMember(player.getUuid()) != null) {
            player.sendMessage(Text.translatable("mctd.msg.already_in_game").formatted(Formatting.RED), false);
            return false;
        }
        session.addMember(player);
        session.broadcast(Text.translatable("mctd.msg.joined", player.getDisplayName()).formatted(Formatting.GREEN));
        session.enter(player);
        return true;
    }

    /** Leaves the arena; the game stays (paused while nobody is inside). */
    public static void leave(ServerPlayerEntity player) {
        Session session = byMember(player.getUuid());
        if (session == null) {
            player.sendMessage(Text.translatable("mctd.msg.not_in_game").formatted(Formatting.RED), false);
            return;
        }
        if (player.getWorld() == session.world()) {
            session.exit(player);
        }
        player.sendMessage(Text.translatable("mctd.msg.left").formatted(Formatting.YELLOW), false);
    }

    /** Leaves the game for good; the game ends when its last member quits. */
    public static void quit(ServerPlayerEntity player) {
        Session session = byMember(player.getUuid());
        if (session == null) {
            player.sendMessage(Text.translatable("mctd.msg.not_in_game").formatted(Formatting.RED), false);
            return;
        }
        if (player.getWorld() == session.world()) {
            session.exit(player);
        }
        session.removeMember(player.getUuid());
        player.sendMessage(Text.translatable("mctd.msg.quit").formatted(Formatting.YELLOW), false);
        if (session.members().isEmpty()) {
            stop(session);
        } else {
            session.broadcast(Text.translatable("mctd.msg.member_quit", player.getDisplayName()).formatted(Formatting.YELLOW));
        }
    }

    public static void stop(Session session) {
        session.broadcast(Text.translatable("mctd.msg.stopped").formatted(Formatting.YELLOW));
        session.dispose(true);
        SESSIONS.remove(session);
        if (state != null) {
            state.markDirty();
        }
    }

    public static void returnToArena(ServerPlayerEntity player) {
        Session session = byMember(player.getUuid());
        if (session == null) {
            player.sendMessage(Text.translatable("mctd.msg.not_in_game").formatted(Formatting.RED), false);
            return;
        }
        session.enter(player);
    }
}
