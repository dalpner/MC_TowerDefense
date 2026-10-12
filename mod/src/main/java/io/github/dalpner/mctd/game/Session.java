package io.github.dalpner.mctd.game;

import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.core.Ability;
import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.AttackStyle;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.WaveDef;
import io.github.dalpner.mctd.core.WaveGenerator;
import io.github.dalpner.mctd.core.sim.Creep;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.GameConfig;
import io.github.dalpner.mctd.core.sim.GameListener;
import io.github.dalpner.mctd.core.sim.Tower;
import io.github.dalpner.mctd.net.FxPayload;
import io.github.dalpner.mctd.net.HudPayload;
import io.github.dalpner.mctd.registry.ModBlocks;
import io.github.dalpner.mctd.registry.ModItems;
import io.github.dalpner.mctd.util.Sounds;
import io.github.dalpner.mctd.util.Texts;
import io.github.dalpner.mctd.world.ArenaBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One running tower defense game in one arena. The pure-Java {@link Game} does the simulation; this class
 * shows it in the world: creeps are vanilla mobs without AI that are moved along the path every tick,
 * towers are blocks, and all effects are sent to the clients as {@link FxPayload}s.
 */
public final class Session implements GameListener {
    public static final String CREEP_TAG = "mctd_creep";
    private static final double PATH_TOP = 0.9375;
    private static final double AIR_HEIGHT = 3.0;
    private static final int MAX_FX_PER_TICK = 300;

    private final MinecraftServer server;
    private final ServerWorld world;
    private final int slot;
    private final BlockPos origin;
    private Game game;
    private UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();
    private final Map<UUID, TowerType> lastBuilt = new HashMap<>();
    private final ServerBossBar bossBar;
    private final Map<Integer, MobEntity> bodies = new HashMap<>();
    private final Map<Integer, Integer> nameStates = new HashMap<>();
    private final Map<Integer, Integer> lastHurt = new HashMap<>();
    private final List<FxPayload.Entry> fx = new ArrayList<>();
    private boolean paused = true;
    private int age;

    Session(MinecraftServer server, ServerWorld world, int slot, BlockPos origin, Game game, UUID owner) {
        this.server = server;
        this.world = world;
        this.slot = slot;
        this.origin = origin;
        this.owner = owner;
        this.members.add(owner);
        this.bossBar = new ServerBossBar(Text.translatable("mctd.bossbar.waiting"), BossBar.Color.WHITE,
                BossBar.Style.NOTCHED_10);
        setGame(game);
    }

    private void setGame(Game game) {
        this.game = game;
        game.setListener(this);
    }

    // ================================================================== accessors

    public Game game() {
        return game;
    }

    public int slot() {
        return slot;
    }

    public BlockPos origin() {
        return origin;
    }

    public ServerWorld world() {
        return world;
    }

    public ArenaLayout layout() {
        return game.config().layout();
    }

    public UUID owner() {
        return owner;
    }

    public Set<UUID> members() {
        return members;
    }

    Map<UUID, ReturnPoint> returnPoints() {
        return returns;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean paused() {
        return paused;
    }

    public TowerType lastBuilt(UUID player) {
        return lastBuilt.get(player);
    }

    /** Whether a block position lies inside this arena (including the border). */
    public boolean contains(BlockPos pos) {
        int x = pos.getX() - origin.getX();
        int z = pos.getZ() - origin.getZ();
        return x >= 0 && z >= 0 && x < layout().size() && z < layout().size()
                && pos.getY() >= origin.getY() - 3 && pos.getY() <= origin.getY() + 20;
    }

    public BlockPos towerPos(Tower t) {
        return origin.add(t.cellX(), 1, t.cellZ());
    }

    public Tower towerAt(BlockPos pos) {
        if (pos.getY() != origin.getY() + 1) {
            return null;
        }
        return game.towerAt(pos.getX() - origin.getX(), pos.getZ() - origin.getZ());
    }

    public Vec3d spawnPoint() {
        int[] cell = layout().playerSpawn();
        return new Vec3d(origin.getX() + cell[0] + 0.5, origin.getY() + 1, origin.getZ() + cell[1] + 0.5);
    }

    /** Arena centre, used to decide which players see the effects. */
    public Vec3d center() {
        double half = layout().size() / 2.0;
        return new Vec3d(origin.getX() + half, origin.getY() + 1, origin.getZ() + half);
    }

    // ================================================================== players

    public List<ServerPlayerEntity> onlineMembers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (UUID uuid : members) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    /** Online members that are inside the arena dimension. */
    public List<ServerPlayerEntity> presentMembers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (ServerPlayerEntity p : onlineMembers()) {
            if (p.getWorld() == world) {
                list.add(p);
            }
        }
        return list;
    }

    /** Players near this arena (members and visitors) who should see effects. */
    private List<ServerPlayerEntity> viewers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        Vec3d c = center();
        double r = layout().size() + 48;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getWorld() == world && p.squaredDistanceTo(c) < r * r) {
                list.add(p);
            }
        }
        return list;
    }

    public void addMember(ServerPlayerEntity player) {
        members.add(player.getUuid());
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        lastBuilt.remove(uuid);
        if (uuid.equals(owner) && !members.isEmpty()) {
            owner = members.iterator().next();
        }
    }

    /** Teleports a member into the arena. */
    public void enter(ServerPlayerEntity player) {
        if (player.getWorld() != world) {
            returns.put(player.getUuid(), ReturnPoint.of(player));
        }
        Vec3d spawn = spawnPoint();
        player.teleport(world, spawn.x, spawn.y, spawn.z, 0f, 20f);
        player.fallDistance = 0;
        onArrive(player);
    }

    /** Called whenever a member is (again) inside the arena dimension. */
    public void onArrive(ServerPlayerEntity player) {
        applyArenaMode(player);
        giveWand(player);
        bossBar.addPlayer(player);
        sendHud(player);
    }

    /** Teleports a member back to where they came from. */
    public void exit(ServerPlayerEntity player) {
        ReturnPoint point = returns.remove(player.getUuid());
        if (point == null) {
            point = ReturnPoint.spawn(server);
        }
        bossBar.removePlayer(player);
        sendHud(player, HudPayload.inactive());
        point.teleport(player, server);
        point.restoreMode(player);
    }

    /** The member left the arena dimension by other means (commands, portals...). */
    public void onDepart(ServerPlayerEntity player) {
        bossBar.removePlayer(player);
        sendHud(player, HudPayload.inactive());
        ReturnPoint point = returns.remove(player.getUuid());
        if (point != null) {
            point.restoreMode(player);
        }
    }

    public void onDisconnect(ServerPlayerEntity player) {
        bossBar.removePlayer(player);
    }

    private static void applyArenaMode(ServerPlayerEntity player) {
        GameMode mode = player.interactionManager.getGameMode();
        if (mode == GameMode.SURVIVAL) {
            player.changeGameMode(GameMode.ADVENTURE);
        }
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
    }

    private static void giveWand(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(ModItems.BUILDER_WAND)) {
                return;
            }
        }
        player.getInventory().offerOrDrop(new ItemStack(ModItems.BUILDER_WAND));
    }

    // ================================================================== tick

    public void tick() {
        age++;
        List<ServerPlayerEntity> present = presentMembers();
        boolean nowPaused = present.isEmpty();
        if (nowPaused != paused) {
            paused = nowPaused;
            if (!paused && world.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                server.setDifficulty(net.minecraft.world.Difficulty.EASY, true);
                broadcast(Text.translatable("mctd.msg.peaceful").formatted(Formatting.YELLOW));
            }
        }
        if (!paused && !game.isOver()) {
            game.tick();
            syncBodies();
        }
        if (age % 10 == 0) {
            for (ServerPlayerEntity p : present) {
                protect(p);
            }
            for (ServerPlayerEntity p : present) {
                sendHud(p);
            }
        }
        if (age % 5 == 0) {
            updateBossBar();
        }
        if (age % 40 == 0) {
            validateTowers();
        }
        flushFx();
    }

    private void protect(ServerPlayerEntity p) {
        if (p.getY() < origin.getY() - 8) {
            Vec3d spawn = spawnPoint();
            p.teleport(world, spawn.x, spawn.y, spawn.z, p.getYaw(), p.getPitch());
            p.fallDistance = 0;
        }
        p.getHungerManager().setFoodLevel(20);
        if (!p.getAbilities().allowFlying) {
            applyArenaMode(p);
        }
    }

    private void validateTowers() {
        for (Tower t : new ArrayList<>(game.towers())) {
            BlockState state = world.getBlockState(towerPos(t));
            if (!(state.getBlock() instanceof TowerBlock tb) || !tb.towerId().equals(t.type().id())) {
                game.sell(t);
                MctdMod.LOGGER.info("Tower {} in arena {} vanished and was removed", t, slot);
            }
        }
    }

    // ================================================================== creep bodies

    private double creepY(Creep c) {
        return c.flying() ? origin.getY() + 1 + AIR_HEIGHT : origin.getY() + PATH_TOP;
    }

    private Vec3d creepPos(Creep c) {
        return new Vec3d(origin.getX() + c.x(), creepY(c), origin.getZ() + c.z());
    }

    private Vec3d creepCenter(Creep c) {
        MobEntity body = bodies.get(c.id());
        double h = body != null ? body.getHeight() * 0.6 : 0.9;
        return creepPos(c).add(0, h, 0);
    }

    private static float yaw(double fx, double fz) {
        return (float) (Math.toDegrees(Math.atan2(fz, fx)) - 90.0);
    }

    private void syncBodies() {
        for (Creep c : game.creeps()) {
            if (c.removed()) {
                continue;
            }
            MobEntity body = bodies.get(c.id());
            if (body == null || body.isRemoved()) {
                body = spawnBody(c);
                if (body == null) {
                    continue;
                }
            }
            Vec3d p = creepPos(c);
            float yaw = yaw(c.facingX(), c.facingZ());
            body.setPosition(p.x, p.y, p.z);
            body.setYaw(yaw);
            body.setHeadYaw(yaw);
            body.setBodyYaw(yaw);
            updateName(c, body);
            body.setFireTicks(c.burning() ? 10 : 0);
            body.setGlowing(c.cursed() || c.spec().bossLike());
            if ((age + c.id()) % 10 == 0) {
                statusParticles(c, body);
            }
        }
    }

    private void statusParticles(Creep c, MobEntity body) {
        double x = body.getX();
        double y = body.getY() + body.getHeight();
        double z = body.getZ();
        if (c.stunned()) {
            particles(ParticleTypes.CRIT, x, y + 0.2, z, 4, 0.3);
        }
        if (c.slowed()) {
            particles(ParticleTypes.SNOWFLAKE, x, y - 0.3, z, 2, 0.3);
        }
        if (c.poisoned()) {
            particles(ParticleTypes.ITEM_SLIME, x, y - 0.5, z, 2, 0.3);
        }
        if (c.diseased()) {
            particles(ParticleTypes.MYCELIUM, x, y - 0.5, z, 4, 0.4);
        }
        if (c.has(Ability.REGEN) && c.hpFraction() < 1) {
            particles(ParticleTypes.HAPPY_VILLAGER, x, y - 0.3, z, 1, 0.3);
        }
        if (c.reviving()) {
            particles(ParticleTypes.SOUL, x, body.getY() + 0.3, z, 2, 0.4);
        }
    }

    private void particles(ParticleEffect effect, double x, double y, double z, int count, double spread) {
        world.spawnParticles(effect, x, y, z, count, spread, spread * 0.5, spread, 0.0);
    }

    private MobEntity spawnBody(Creep c) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(c.spec().mob()));
        Entity entity = type.create(world);
        if (!(entity instanceof MobEntity)) {
            if (entity != null) {
                entity.discard();
            }
            entity = EntityType.ZOMBIE.create(world);
        }
        if (!(entity instanceof MobEntity mob)) {
            return null;
        }
        if (mob instanceof BatEntity bat) {
            bat.setRoosting(false);
        }
        mob.setAiDisabled(true);
        mob.setInvulnerable(true);
        mob.setPersistent();
        mob.setSilent(true);
        mob.addCommandTag(CREEP_TAG);
        EntityAttributeInstance scale = mob.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (scale != null) {
            scale.setBaseValue(c.spec().scale());
        }
        Vec3d p = creepPos(c);
        float yaw = yaw(c.facingX(), c.facingZ());
        mob.refreshPositionAndAngles(p.x, p.y, p.z, yaw, 0f);
        mob.setHeadYaw(yaw);
        mob.setBodyYaw(yaw);
        mob.setCustomName(creepName(c));
        mob.setCustomNameVisible(true);
        mob.setGlowing(c.spec().bossLike());
        SessionManager.trackBody(mob.getUuid());
        world.spawnEntity(mob);
        joinTeam(mob, c.element());
        bodies.put(c.id(), mob);
        nameStates.put(c.id(), nameState(c));
        return mob;
    }

    private void removeBody(Creep c, boolean dying) {
        MobEntity body = bodies.remove(c.id());
        nameStates.remove(c.id());
        lastHurt.remove(c.id());
        if (body == null) {
            return;
        }
        leaveTeam(body);
        SessionManager.untrackBody(body.getUuid());
        if (dying && !body.isRemoved()) {
            body.setGlowing(false);
            body.setFireTicks(0);
            body.setCustomName(Text.literal("+" + c.goldReward() + " ⛃").formatted(Formatting.GOLD));
            body.setHealth(0f);
        } else {
            body.discard();
        }
    }

    public void discardBodies() {
        for (MobEntity body : bodies.values()) {
            leaveTeam(body);
            SessionManager.untrackBody(body.getUuid());
            body.discard();
        }
        bodies.clear();
        nameStates.clear();
        lastHurt.clear();
    }

    private void joinTeam(MobEntity mob, Element element) {
        Scoreboard scoreboard = server.getScoreboard();
        String name = "mctd_" + (element == null ? "neutral" : element.id());
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.addTeam(name);
            team.setColor(Texts.color(element));
            team.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        }
        scoreboard.addScoreHolderToTeam(mob.getNameForScoreboard(), team);
    }

    private void leaveTeam(MobEntity mob) {
        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getScoreHolderTeam(mob.getNameForScoreboard());
        if (team != null && team.getName().startsWith("mctd_")) {
            scoreboard.removeScoreHolderFromTeam(mob.getNameForScoreboard(), team);
        }
    }

    private static int nameState(Creep c) {
        int filled = (int) Math.ceil(c.hpFraction() * 10);
        return filled + (c.reviving() ? 100 : 0);
    }

    private void updateName(Creep c, MobEntity body) {
        int state = nameState(c);
        Integer old = nameStates.get(c.id());
        if (old == null || old != state) {
            nameStates.put(c.id(), state);
            body.setCustomName(creepName(c));
        }
    }

    private static Text creepName(Creep c) {
        Formatting color = Texts.color(c.element());
        if (c.reviving()) {
            return Text.literal("☠ ☠ ☠").formatted(Formatting.DARK_GRAY);
        }
        int filled = Math.max(0, Math.min(10, (int) Math.ceil(c.hpFraction() * 10)));
        MutableText text = Text.literal(Texts.symbol(c.element()) + " ").formatted(color);
        if (c.spec().bossLike()) {
            text = Text.literal("♛ ").formatted(Formatting.GOLD).append(text);
        }
        text.append(Text.literal("▌".repeat(filled)).formatted(color));
        text.append(Text.literal("▌".repeat(10 - filled)).formatted(Formatting.DARK_GRAY));
        for (Ability a : c.spec().abilities()) {
            text.append(Text.literal(" " + Texts.symbol(a)).formatted(Formatting.LIGHT_PURPLE));
        }
        return text;
    }

    // ================================================================== game listener

    @Override
    public void creepSpawned(Creep creep) {
        if (spawnBody(creep) != null) {
            Vec3d p = creepPos(creep);
            addFx(FxPayload.SPAWN, creep.element(), FxPayload.STYLE_ELEMENT, p, p, 1f);
        }
    }

    @Override
    public void creepDamaged(Creep creep, Tower source, double amount, boolean crit) {
        MobEntity body = bodies.get(creep.id());
        if (body == null || source == null) {
            return;
        }
        Integer last = lastHurt.get(creep.id());
        if (last == null || age - last >= 10) {
            lastHurt.put(creep.id(), age);
            world.sendEntityDamage(body, world.getDamageSources().generic());
        }
        if (crit) {
            particles(ParticleTypes.ENCHANTED_HIT, body.getX(), body.getY() + body.getHeight() * 0.7, body.getZ(), 6, 0.3);
        }
    }

    @Override
    public void creepDied(Creep creep, Tower killer) {
        Vec3d p = creepCenter(creep);
        removeBody(creep, true);
        addFx(FxPayload.DEATH, creep.element(), FxPayload.STYLE_ELEMENT, p, p, creep.goldReward());
    }

    @Override
    public void creepLeaked(Creep creep) {
        Vec3d p = creepCenter(creep);
        removeBody(creep, false);
        addFx(FxPayload.LEAK, creep.element(), FxPayload.STYLE_ELEMENT, p, p, creep.spec().livesCost());
        MutableText msg = Text.translatable("mctd.msg.leak", creep.spec().livesCost(), game.lives())
                .formatted(Formatting.RED);
        for (ServerPlayerEntity player : onlineMembers()) {
            player.sendMessage(msg, true);
        }
    }

    @Override
    public void creepFell(Creep creep) {
        MobEntity body = bodies.get(creep.id());
        if (body != null) {
            body.setCustomName(creepName(creep));
            nameStates.put(creep.id(), nameState(creep));
        }
        Vec3d p = creepCenter(creep);
        addFx(FxPayload.FALL, creep.element(), FxPayload.STYLE_ELEMENT, p, p, 1f);
    }

    @Override
    public void creepRose(Creep creep) {
        Vec3d p = creepCenter(creep);
        addFx(FxPayload.RISE, creep.element(), FxPayload.STYLE_ELEMENT, p, p, 1f);
    }

    @Override
    public void creepsHealed(Creep healer, List<Creep> healed) {
        int n = 0;
        for (Creep c : healed) {
            if (n++ >= 8) {
                break;
            }
            Vec3d p = creepCenter(c);
            addFx(FxPayload.HEAL, c.element(), FxPayload.STYLE_ELEMENT, creepCenter(healer), p, 1f);
        }
    }

    @Override
    public void towerAttacked(Tower tower, List<Creep> targets) {
        if (targets.isEmpty()) {
            return;
        }
        TowerType type = tower.type();
        Creep first = targets.get(0);
        Element element = type.damageElementAgainst(first.element());
        byte style = type.elements().isEmpty()
                ? (type.style() == AttackStyle.SPLASH ? FxPayload.STYLE_CANNON : FxPayload.STYLE_ARROW)
                : FxPayload.STYLE_ELEMENT;
        Vec3d from = towerTop(tower);
        switch (type.style()) {
            case CHAIN -> {
                Vec3d prev = from;
                for (Creep c : targets) {
                    Vec3d p = creepCenter(c);
                    addFx(FxPayload.CHAIN, element, style, prev, p, tower.tier());
                    prev = p;
                }
            }
            case PULSE -> addFx(FxPayload.PULSE, element, style, from, from, (float) tower.stats().range);
            case SPLASH -> {
                Vec3d p = creepCenter(first);
                addFx(FxPayload.BEAM, element, style, from, p, tower.tier());
                addFx(FxPayload.SPLASH, element, style, p, p, (float) tower.stats().splashRadius);
            }
            default -> {
                for (Creep c : targets) {
                    addFx(FxPayload.BEAM, element, style, from, creepCenter(c), tower.tier());
                }
            }
        }
    }

    @Override
    public void waveStarted(WaveDef wave) {
        MutableText title = Text.translatable("mctd.title.wave", wave.number()).formatted(Formatting.GOLD);
        MutableText subtitle = Texts.element(wave.element()).append(Text.literal(" · ").formatted(Formatting.GRAY))
                .append(Texts.kind(wave.kind()).formatted(wave.kind().bossLike() ? Formatting.DARK_RED : Formatting.WHITE));
        for (ServerPlayerEntity p : presentMembers()) {
            title(p, title, subtitle);
        }
        broadcast(Text.translatable("mctd.msg.wave_start", Texts.wave(wave), wave.count()).formatted(Formatting.GRAY));
        Sounds.play(world, center(), wave.kind().bossLike() ? "entity.ender_dragon.growl" : "event.raid.horn", 0.6f, 1.2f);
    }

    @Override
    public void waveCleared(int wave, int bonus, int interest, int towerGold) {
        MutableText msg = Text.translatable("mctd.msg.wave_cleared", wave, bonus + interest + towerGold, interest)
                .formatted(Formatting.GREEN);
        broadcast(msg);
        Sounds.play(world, center(), "entity.player.levelup", 0.5f, 1.4f);
    }

    @Override
    public void countdownStarted(int nextWave, int ticks) {
        if (nextWave <= game.maxWaves() || game.endless()) {
            WaveDef next = game.waves().wave(nextWave);
            broadcast(Text.translatable("mctd.msg.next_wave", Texts.wave(next), Texts.seconds(ticks))
                    .formatted(Formatting.GRAY));
        }
    }

    @Override
    public void essenceGained(int essences) {
        broadcast(Text.translatable("mctd.msg.essence", essences).formatted(Formatting.AQUA));
        for (ServerPlayerEntity p : presentMembers()) {
            title(p, Text.empty(), Text.translatable("mctd.title.essence").formatted(Formatting.AQUA));
        }
        Sounds.play(world, center(), "block.beacon.power_select", 0.6f, 1.2f);
    }

    @Override
    public void elementBought(Element element, int level, Creep guardian) {
        broadcast(Text.translatable("mctd.msg.element_bought", Texts.element(element), level).formatted(Formatting.AQUA));
        if (guardian != null) {
            broadcast(Text.translatable("mctd.msg.guardian", Texts.element(element)).formatted(Formatting.YELLOW));
        }
        Vec3d c = spawnPoint();
        addFx(FxPayload.ELEMENT, element, FxPayload.STYLE_ELEMENT, c, c, 3f);
    }

    @Override
    public void gameOver(boolean victory) {
        discardBodies();
        MutableText title = Text.translatable(victory ? "mctd.title.victory" : "mctd.title.defeat")
                .formatted(victory ? Formatting.GOLD : Formatting.DARK_RED);
        MutableText subtitle = Text.translatable("mctd.title.stats", game.currentWave(), game.kills())
                .formatted(Formatting.GRAY);
        for (ServerPlayerEntity p : onlineMembers()) {
            title(p, title, subtitle);
        }
        broadcast(Text.translatable(victory ? "mctd.msg.victory" : "mctd.msg.defeat", game.currentWave(),
                game.kills(), game.leaks(), game.goldEarned()).formatted(victory ? Formatting.GOLD : Formatting.RED));
        Sounds.play(world, center(), victory ? "ui.toast.challenge_complete" : "entity.wither.death", 0.8f, 1f);
        updateBossBar();
    }

    // ================================================================== effects

    private Vec3d towerTop(Tower t) {
        return new Vec3d(origin.getX() + t.cellX() + 0.5, origin.getY() + 1.9, origin.getZ() + t.cellZ() + 0.5);
    }

    private void addFx(byte type, Element element, byte style, Vec3d a, Vec3d b, float size) {
        if (fx.size() >= MAX_FX_PER_TICK) {
            return;
        }
        fx.add(new FxPayload.Entry(type, (byte) (element == null ? -1 : element.ordinal()), style,
                (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, size));
    }

    private void flushFx() {
        if (fx.isEmpty()) {
            return;
        }
        FxPayload payload = new FxPayload(new ArrayList<>(fx));
        fx.clear();
        for (ServerPlayerEntity p : viewers()) {
            if (ServerPlayNetworking.canSend(p, FxPayload.ID)) {
                ServerPlayNetworking.send(p, payload);
            }
        }
    }

    // ================================================================== player actions

    public TdResult build(ServerPlayerEntity player, TowerType type, BlockPos pos) {
        if (!contains(pos) || pos.getY() != origin.getY() + 1) {
            return TdResult.NOT_BUILDABLE;
        }
        BlockState current = world.getBlockState(pos);
        if (!current.isAir() && !current.isReplaceable()) {
            return TdResult.OCCUPIED;
        }
        if (!world.getEntitiesByClass(PlayerEntity.class, new Box(pos), p -> !p.isSpectator()).isEmpty()) {
            return TdResult.OCCUPIED;
        }
        int x = pos.getX() - origin.getX();
        int z = pos.getZ() - origin.getZ();
        TdResult result = game.canBuild(type, x, z);
        if (result != TdResult.OK) {
            return result;
        }
        Tower tower = game.build(type, x, z);
        world.setBlockState(pos, ModBlocks.tower(type).withTier(1), Block.NOTIFY_ALL);
        lastBuilt.put(player.getUuid(), type);
        Vec3d c = Vec3d.ofBottomCenter(pos);
        addFx(FxPayload.BUILD, type.damageElement(), FxPayload.STYLE_ELEMENT, c, c, tower.tier());
        return TdResult.OK;
    }

    public TdResult upgrade(Tower tower) {
        TdResult result = game.upgrade(tower);
        if (result == TdResult.OK) {
            world.setBlockState(towerPos(tower), ModBlocks.tower(tower.type()).withTier(tower.tier()), Block.NOTIFY_ALL);
            Vec3d c = Vec3d.ofBottomCenter(towerPos(tower));
            addFx(FxPayload.UPGRADE, tower.type().damageElement(), FxPayload.STYLE_ELEMENT, c, c, tower.tier());
        }
        return result;
    }

    public int sell(Tower tower) {
        int value = game.sell(tower);
        if (value >= 0) {
            world.setBlockState(towerPos(tower), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            Vec3d c = Vec3d.ofBottomCenter(towerPos(tower));
            addFx(FxPayload.SELL, tower.type().damageElement(), FxPayload.STYLE_ELEMENT, c, c, value);
        }
        return value;
    }

    /** Places the blocks of all towers of the game (after loading or restarting). */
    public void placeTowerBlocks() {
        for (Tower t : game.towers()) {
            world.setBlockState(towerPos(t), ModBlocks.tower(t.type()).withTier(t.tier()), Block.NOTIFY_ALL);
        }
    }

    /** Starts a fresh game with the same settings in the same arena. */
    public void restart(long seed) {
        discardBodies();
        GameConfig old = game.config();
        ArenaBuilder.build(world, origin, old.layout(), seed);
        setGame(new Game(new GameConfig(old.difficulty(), old.layout(), seed, old.maxWaves(), old.elementGuardians())));
        broadcast(Text.translatable("mctd.msg.restarted").formatted(Formatting.GREEN));
        for (ServerPlayerEntity p : onlineMembers()) {
            sendHud(p);
        }
    }

    /** Ends the session: everybody is sent home and the arena is removed. */
    public void dispose(boolean clearArena) {
        discardBodies();
        for (ServerPlayerEntity p : onlineMembers()) {
            if (p.getWorld() == world) {
                exit(p);
            } else {
                sendHud(p, HudPayload.inactive());
            }
        }
        bossBar.clearPlayers();
        if (clearArena) {
            ArenaBuilder.clear(world, origin, layout());
            ArenaBuilder.loadChunks(world, origin, layout(), false);
        }
    }

    // ================================================================== HUD

    private void updateBossBar() {
        MutableText name;
        float percent;
        BossBar.Color color = BossBar.Color.WHITE;
        if (game.phase() == Game.Phase.VICTORY) {
            name = Text.translatable("mctd.bossbar.victory").formatted(Formatting.GOLD);
            percent = 1f;
            color = BossBar.Color.YELLOW;
        } else if (game.phase() == Game.Phase.DEFEAT) {
            name = Text.translatable("mctd.bossbar.defeat").formatted(Formatting.DARK_RED);
            percent = 0f;
            color = BossBar.Color.RED;
        } else if (paused) {
            name = Text.translatable("mctd.bossbar.paused").formatted(Formatting.GRAY);
            percent = 1f;
        } else if (game.phase() == Game.Phase.BUILD) {
            name = Text.translatable("mctd.bossbar.build", game.nextWave(), Texts.seconds(game.countdown()))
                    .formatted(Formatting.GREEN);
            percent = Math.max(0f, Math.min(1f, game.countdown() / (float) WaveGenerator.buildTicks(game.nextWave())));
            color = BossBar.Color.GREEN;
        } else {
            WaveDef wave = game.waves().wave(Math.max(1, game.currentWave()));
            name = Text.translatable("mctd.bossbar.wave", Texts.wave(wave), game.waveCreepsLeft());
            percent = Math.max(0f, Math.min(1f, game.waveCreepsLeft() / (float) Math.max(1, wave.count())));
            color = barColor(wave.element());
        }
        bossBar.setName(name);
        bossBar.setPercent(percent);
        bossBar.setColor(color);
    }

    private static BossBar.Color barColor(Element element) {
        if (element == null) {
            return BossBar.Color.PURPLE;
        }
        return switch (element) {
            case LIGHT -> BossBar.Color.WHITE;
            case DARKNESS -> BossBar.Color.PURPLE;
            case WATER -> BossBar.Color.BLUE;
            case FIRE -> BossBar.Color.RED;
            case NATURE -> BossBar.Color.GREEN;
            case EARTH -> BossBar.Color.YELLOW;
        };
    }

    public void sendHud(ServerPlayerEntity player) {
        sendHud(player, hud());
    }

    private static void sendHud(ServerPlayerEntity player, HudPayload payload) {
        if (ServerPlayNetworking.canSend(player, HudPayload.ID)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private HudPayload hud() {
        byte phase = switch (game.phase()) {
            case BUILD -> HudPayload.PHASE_BUILD;
            case WAVE -> HudPayload.PHASE_WAVE;
            case VICTORY -> HudPayload.PHASE_VICTORY;
            case DEFEAT -> HudPayload.PHASE_DEFEAT;
        };
        byte[] levels = new byte[Element.values().length];
        for (Element e : Element.values()) {
            levels[e.ordinal()] = (byte) game.level(e);
        }
        List<HudPayload.WaveInfo> upcoming = new ArrayList<>();
        for (int n = game.nextWave(); n < game.nextWave() + 3; n++) {
            if (n > game.maxWaves() && !game.endless()) {
                break;
            }
            WaveDef def = game.waves().wave(n);
            int mask = 0;
            for (Ability a : def.spec().abilities()) {
                mask |= 1 << a.ordinal();
            }
            upcoming.add(new HudPayload.WaveInfo(n, (byte) (def.element() == null ? -1 : def.element().ordinal()),
                    (byte) def.kind().ordinal(), (byte) mask, def.count()));
        }
        Difficulty difficulty = game.config().difficulty();
        return new HudPayload(true, paused, game.currentWave(), game.maxWaves(), game.endless(), phase,
                game.countdown(), game.gold(), game.lives(), difficulty.lives(), game.essences(), levels,
                game.waveCreepsLeft(), difficulty.id(), layout().id(), upcoming);
    }

    // ================================================================== messages

    public void broadcast(Text text) {
        for (ServerPlayerEntity p : onlineMembers()) {
            p.sendMessage(text, false);
        }
    }

    private static void title(ServerPlayerEntity player, Text title, Text subtitle) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
    }
}
