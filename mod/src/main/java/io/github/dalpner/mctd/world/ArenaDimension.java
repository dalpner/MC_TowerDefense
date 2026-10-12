package io.github.dalpner.mctd.world;

import io.github.dalpner.mctd.MctdMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * The void dimension all arenas are built in (defined by the data pack files
 * {@code data/mctd/dimension/arena.json} and {@code dimension_type/arena.json}).
 * It has a fixed time of day, so undead creeps do not burn in the sun.
 */
public final class ArenaDimension {
    public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, MctdMod.id("arena"));

    /** Arenas are placed next to each other along the x axis. */
    public static final int SLOT_SPACING = 256;
    public static final int FLOOR_Y = 64;

    private ArenaDimension() {
    }

    public static ServerWorld get(MinecraftServer server) {
        return server.getWorld(WORLD);
    }

    public static boolean is(World world) {
        return world.getRegistryKey() == WORLD;
    }
}
