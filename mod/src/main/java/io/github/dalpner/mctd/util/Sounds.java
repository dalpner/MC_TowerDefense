package io.github.dalpner.mctd.util;

import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Looks sounds up by id so the code does not depend on how vanilla declares its sound constants. */
public final class Sounds {
    private Sounds() {
    }

    public static SoundEvent get(String id) {
        return Registries.SOUND_EVENT.get(Identifier.ofVanilla(id));
    }

    public static void play(ServerWorld world, Vec3d pos, String id, float volume, float pitch) {
        SoundEvent sound = get(id);
        if (sound != null) {
            world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, pitch);
        }
    }

    public static void play(ServerPlayerEntity player, String id, float volume, float pitch) {
        if (player.getWorld() instanceof ServerWorld world) {
            play(world, player.getPos(), id, volume, pitch);
        }
    }
}
