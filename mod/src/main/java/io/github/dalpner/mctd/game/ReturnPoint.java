package io.github.dalpner.mctd.game;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

/** Where a player came from before entering an arena, and their game mode at that time. */
public record ReturnPoint(RegistryKey<World> dimension, double x, double y, double z, float yaw, float pitch,
                          GameMode mode) {

    public static ReturnPoint of(ServerPlayerEntity player) {
        return new ReturnPoint(player.getWorld().getRegistryKey(), player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), player.interactionManager.getGameMode());
    }

    /** Fallback for players without a stored point: world spawn and the server's default game mode. */
    public static ReturnPoint spawn(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return new ReturnPoint(World.OVERWORLD, overworld.getSpawnPos().getX() + 0.5, overworld.getSpawnPos().getY(),
                overworld.getSpawnPos().getZ() + 0.5, 0, 0, server.getDefaultGameMode());
    }

    public void teleport(ServerPlayerEntity player, MinecraftServer server) {
        ServerWorld target = server.getWorld(dimension);
        if (target == null) {
            target = server.getOverworld();
        }
        player.teleport(target, x, y, z, yaw, pitch);
        player.fallDistance = 0;
    }

    public void restoreMode(ServerPlayerEntity player) {
        if (player.interactionManager.getGameMode() != mode) {
            player.changeGameMode(mode);
        }
        player.getAbilities().allowFlying = mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().flying = false;
        }
        player.sendAbilitiesUpdate();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("dim", dimension.getValue().toString());
        nbt.putDouble("x", x);
        nbt.putDouble("y", y);
        nbt.putDouble("z", z);
        nbt.putFloat("yaw", yaw);
        nbt.putFloat("pitch", pitch);
        nbt.putString("mode", mode.name());
        return nbt;
    }

    public static ReturnPoint fromNbt(NbtCompound nbt) {
        Identifier dim = Identifier.tryParse(nbt.getString("dim"));
        RegistryKey<World> key = dim == null ? World.OVERWORLD : RegistryKey.of(RegistryKeys.WORLD, dim);
        GameMode mode;
        try {
            mode = GameMode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException e) {
            mode = GameMode.SURVIVAL;
        }
        return new ReturnPoint(key, nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"),
                nbt.getFloat("yaw"), nbt.getFloat("pitch"), mode);
    }
}
