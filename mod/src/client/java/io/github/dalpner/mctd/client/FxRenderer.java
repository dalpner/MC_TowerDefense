package io.github.dalpner.mctd.client;

import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.net.FxPayload;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/** Turns {@link FxPayload} entries into particles and sounds. */
public final class FxRenderer {
    private static final Random RANDOM = Random.create();
    private static final Map<String, Long> SOUND_COOLDOWN = new HashMap<>();
    private static int soundsThisBatch;
    private static double density = 1.0;

    private FxRenderer() {
    }

    static void play(FxPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        ParticlesMode mode = client.options.getParticles().getValue();
        density = mode == ParticlesMode.ALL ? 1.0 : mode == ParticlesMode.DECREASED ? 0.5 : 0.2;
        soundsThisBatch = 0;
        for (FxPayload.Entry e : payload.entries()) {
            Element element = e.element() >= 0 && e.element() < Element.values().length ? Element.values()[e.element()] : null;
            Vec3d a = new Vec3d(e.x1(), e.y1(), e.z1());
            Vec3d b = new Vec3d(e.x2(), e.y2(), e.z2());
            switch (e.type()) {
                case FxPayload.BEAM -> beam(world, element, e.style(), a, b, e.size());
                case FxPayload.CHAIN -> chain(world, element, a, b);
                case FxPayload.SPLASH -> splash(world, element, e.style(), b, e.size());
                case FxPayload.PULSE -> pulse(world, element, a, e.size());
                case FxPayload.DEATH -> death(world, a, e.size());
                case FxPayload.LEAK -> leak(world, a);
                case FxPayload.BUILD -> build(world, element, a, false);
                case FxPayload.UPGRADE -> build(world, element, a, true);
                case FxPayload.SELL -> sell(world, a);
                case FxPayload.HEAL -> burst(world, ParticleTypes.HEART, b.add(0, 0.5, 0), 2, 0.3);
                case FxPayload.FALL -> fall(world, a);
                case FxPayload.RISE -> rise(world, a);
                case FxPayload.ELEMENT -> element(world, element, a);
                case FxPayload.SPAWN -> burst(world, ParticleTypes.REVERSE_PORTAL, a.add(0, 1, 0), 8, 0.4);
                default -> {
                }
            }
        }
    }

    // ------------------------------------------------------------------ effects

    private static void beam(ClientWorld world, Element element, byte style, Vec3d from, Vec3d to, float tier) {
        if (style == FxPayload.STYLE_ARROW) {
            line(world, ParticleTypes.CRIT, from, to, 0.5);
            sound(world, to, "entity.arrow.shoot", 0.25f, 1.4f, "arrow", 2);
            return;
        }
        if (style == FxPayload.STYLE_CANNON) {
            line(world, ParticleTypes.SMOKE, from, to, 0.6);
            return;
        }
        line(world, dust(element, 0.7f + tier * 0.25f), from, to, 0.25);
        accent(world, element, to, 2);
        elementSound(world, element, to);
    }

    private static void chain(ClientWorld world, Element element, Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        int steps = Math.max(2, (int) (delta.length() / 0.8));
        Vec3d prev = from;
        for (int i = 1; i <= steps; i++) {
            Vec3d point = from.add(delta.multiply(i / (double) steps));
            if (i < steps) {
                point = point.add(RANDOM.nextGaussian() * 0.25, RANDOM.nextGaussian() * 0.25, RANDOM.nextGaussian() * 0.25);
            }
            line(world, dust(element, 0.8f), prev, point, 0.2);
            prev = point;
        }
        burst(world, ParticleTypes.ELECTRIC_SPARK, to, 4, 0.2);
        sound(world, to, "entity.lightning_bolt.impact", 0.12f, 1.8f, "chain", 3);
    }

    private static void splash(ClientWorld world, Element element, byte style, Vec3d at, float radius) {
        if (style == FxPayload.STYLE_CANNON) {
            burst(world, ParticleTypes.EXPLOSION, at, 1, 0.1);
            burst(world, ParticleTypes.SMOKE, at, 8, radius * 0.4);
            sound(world, at, "entity.generic.explode", 0.2f, 1.6f, "cannon", 3);
            return;
        }
        ring(world, dust(element, 1.0f), at.add(0, -0.4, 0), radius, 18);
        accent(world, element, at, 8);
    }

    private static void pulse(ClientWorld world, Element element, Vec3d at, float radius) {
        Vec3d ground = at.add(0, -0.85, 0);
        ring(world, dust(element, 1.2f), ground, radius, Math.max(24, (int) (radius * 7)));
        ring(world, dust(element, 0.9f), ground, radius * 0.5, 14);
        for (int i = 0; i < 6; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * radius;
            accent(world, element, ground.add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r), 1);
        }
        elementSound(world, element, at);
    }

    private static void death(ClientWorld world, Vec3d at, float gold) {
        burst(world, ParticleTypes.POOF, at, 4, 0.3);
        burst(world, ParticleTypes.WAX_ON, at.add(0, 0.4, 0), Math.min(10, 2 + (int) (gold / 10)), 0.4);
        sound(world, at, "entity.experience_orb.pickup", 0.15f, 1.6f, "death", 2);
    }

    private static void leak(ClientWorld world, Vec3d at) {
        burst(world, ParticleTypes.PORTAL, at, 30, 0.6);
        burst(world, ParticleTypes.REVERSE_PORTAL, at, 15, 0.4);
        sound(world, at, "entity.enderman.teleport", 0.6f, 0.6f, "leak", 4);
    }

    private static void build(ClientWorld world, Element element, Vec3d at, boolean upgrade) {
        Vec3d center = at.add(0, 0.6, 0);
        if (upgrade) {
            burst(world, ParticleTypes.TOTEM_OF_UNDYING, center, 25, 0.4);
        } else {
            burst(world, ParticleTypes.HAPPY_VILLAGER, center, 12, 0.5);
        }
        ring(world, dust(element, 1.0f), at.add(0, 0.05, 0), 0.9, 12);
    }

    private static void sell(ClientWorld world, Vec3d at) {
        burst(world, ParticleTypes.LARGE_SMOKE, at.add(0, 0.6, 0), 8, 0.4);
        burst(world, ParticleTypes.WAX_ON, at.add(0, 0.6, 0), 10, 0.5);
    }

    private static void fall(ClientWorld world, Vec3d at) {
        burst(world, ParticleTypes.SOUL, at, 8, 0.4);
        sound(world, at, "entity.skeleton.death", 0.4f, 0.7f, "fall", 2);
    }

    private static void rise(ClientWorld world, Vec3d at) {
        burst(world, ParticleTypes.SOUL_FIRE_FLAME, at, 12, 0.4);
        burst(world, ParticleTypes.SCULK_SOUL, at, 4, 0.3);
        sound(world, at, "entity.evoker.prepare_summon", 0.4f, 1.2f, "rise", 2);
    }

    private static void element(ClientWorld world, Element element, Vec3d at) {
        for (int i = 0; i < 3; i++) {
            ring(world, dust(element, 1.5f), at.add(0, 0.2 + i * 0.8, 0), 1.5 + i * 0.7, 24);
        }
        burst(world, ParticleTypes.END_ROD, at.add(0, 1.0, 0), 30, 0.8);
        sound(world, at, "block.beacon.activate", 0.8f, 1.2f, "element", 1);
    }

    // ------------------------------------------------------------------ helpers

    private static void accent(ClientWorld world, Element element, Vec3d at, int count) {
        if (element == null) {
            burst(world, ParticleTypes.CRIT, at, count, 0.2);
            return;
        }
        switch (element) {
            case LIGHT -> burst(world, ParticleTypes.END_ROD, at, count, 0.25);
            case DARKNESS -> burst(world, ParticleTypes.SQUID_INK, at, count, 0.2);
            case WATER -> burst(world, ParticleTypes.SPLASH, at, count * 3, 0.3);
            case FIRE -> burst(world, ParticleTypes.FLAME, at, count, 0.25);
            case NATURE -> burst(world, ParticleTypes.HAPPY_VILLAGER, at, count, 0.3);
            case EARTH -> burst(world, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.getDefaultState()),
                    at, count * 2, 0.25);
        }
    }

    private static void elementSound(ClientWorld world, Element element, Vec3d at) {
        if (element == null) {
            return;
        }
        switch (element) {
            case LIGHT -> sound(world, at, "block.amethyst_block.chime", 0.5f, 1.4f, "light", 2);
            case DARKNESS -> sound(world, at, "entity.evoker.cast_spell", 0.15f, 1.6f, "darkness", 3);
            case WATER -> sound(world, at, "entity.player.splash", 0.12f, 1.6f, "water", 3);
            case FIRE -> sound(world, at, "entity.blaze.shoot", 0.15f, 1.4f, "fire", 3);
            case NATURE -> sound(world, at, "block.azalea_leaves.break", 0.4f, 1.2f, "nature", 2);
            case EARTH -> sound(world, at, "block.gravel.break", 0.4f, 0.8f, "earth", 2);
        }
    }

    private static DustParticleEffect dust(Element element, float size) {
        int rgb = element == null ? 0xE6E6E6 : element.color();
        return new DustParticleEffect(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f), Math.min(4f, size));
    }

    private static void line(ClientWorld world, ParticleEffect effect, Vec3d from, Vec3d to, double spacing) {
        Vec3d delta = to.subtract(from);
        double length = delta.length();
        double step = spacing / density;
        for (double d = 0; d <= length; d += step) {
            Vec3d p = from.add(delta.multiply(d / Math.max(0.001, length)));
            world.addParticle(effect, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private static void ring(ClientWorld world, ParticleEffect effect, Vec3d center, double radius, int points) {
        int n = Math.max(6, (int) (points * density));
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n;
            world.addParticle(effect, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius,
                    0, 0.01, 0);
        }
    }

    private static void burst(ClientWorld world, ParticleEffect effect, Vec3d at, int count, double spread) {
        int n = Math.max(1, (int) Math.round(count * density));
        for (int i = 0; i < n; i++) {
            world.addParticle(effect, at.x + RANDOM.nextGaussian() * spread, at.y + RANDOM.nextGaussian() * spread * 0.6,
                    at.z + RANDOM.nextGaussian() * spread, RANDOM.nextGaussian() * 0.02, RANDOM.nextDouble() * 0.05,
                    RANDOM.nextGaussian() * 0.02);
        }
    }

    /** Plays a sound unless too many sounds were played this tick or the same kind of sound is on cooldown. */
    private static void sound(ClientWorld world, Vec3d at, String id, float volume, float pitch, String group, int cooldownTicks) {
        if (soundsThisBatch >= 6) {
            return;
        }
        long now = world.getTime();
        Long last = SOUND_COOLDOWN.get(group);
        if (last != null && now - last < cooldownTicks) {
            return;
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(Identifier.ofVanilla(id));
        if (sound == null) {
            return;
        }
        SOUND_COOLDOWN.put(group, now);
        soundsThisBatch++;
        world.playSound(at.x, at.y, at.z, sound, SoundCategory.PLAYERS, volume,
                pitch * (0.9f + RANDOM.nextFloat() * 0.2f), false);
    }
}
