package io.github.dalpner.mctd.core;

/**
 * Which vanilla mob represents a creep of a given element and kind. The Minecraft layer spawns these
 * mobs with disabled AI and moves them along the path itself.
 */
public final class MobTable {

    /** A vanilla entity id and its size multiplier. */
    public record Body(String mob, float scale) {
    }

    private MobTable() {
    }

    public static Body body(Element element, CreepKind kind) {
        if (kind == CreepKind.FINAL_BOSS || element == null) {
            return new Body("minecraft:giant", 0.55f);
        }
        if (kind == CreepKind.ELEMENTAL) {
            Body normal = body(element, CreepKind.NORMAL);
            return new Body(normal.mob(), normal.scale() * 1.7f);
        }
        return switch (element) {
            case LIGHT -> switch (kind) {
                case FAST -> new Body("minecraft:wolf", 1.0f);
                case MASS -> new Body("minecraft:skeleton", 0.6f);
                case AIR -> new Body("minecraft:ghast", 0.3f);
                case BOSS -> new Body("minecraft:polar_bear", 1.8f);
                default -> new Body("minecraft:skeleton", 1.0f);
            };
            case DARKNESS -> switch (kind) {
                case FAST -> new Body("minecraft:enderman", 0.9f);
                case MASS -> new Body("minecraft:endermite", 1.6f);
                case AIR -> new Body("minecraft:phantom", 1.0f);
                case BOSS -> new Body("minecraft:warden", 1.0f);
                default -> new Body("minecraft:wither_skeleton", 0.85f);
            };
            case WATER -> switch (kind) {
                case FAST -> new Body("minecraft:stray", 1.0f);
                case MASS -> new Body("minecraft:drowned", 0.6f);
                case AIR -> new Body("minecraft:allay", 1.5f);
                case BOSS -> new Body("minecraft:elder_guardian", 1.0f);
                default -> new Body("minecraft:drowned", 1.0f);
            };
            case FIRE -> switch (kind) {
                case FAST -> new Body("minecraft:fox", 1.2f);
                case MASS -> new Body("minecraft:magma_cube", 1.4f);
                case AIR -> new Body("minecraft:blaze", 1.0f);
                case BOSS -> new Body("minecraft:hoglin", 1.5f);
                default -> new Body("minecraft:zombified_piglin", 1.0f);
            };
            case NATURE -> switch (kind) {
                case FAST -> new Body("minecraft:spider", 0.8f);
                case MASS -> new Body("minecraft:cave_spider", 1.0f);
                case AIR -> new Body("minecraft:bee", 1.6f);
                case BOSS -> new Body("minecraft:ravager", 1.0f);
                default -> new Body("minecraft:creeper", 1.0f);
            };
            case EARTH -> switch (kind) {
                case FAST -> new Body("minecraft:silverfish", 2.0f);
                case MASS -> new Body("minecraft:silverfish", 1.3f);
                case AIR -> new Body("minecraft:bat", 2.0f);
                case BOSS -> new Body("minecraft:iron_golem", 1.4f);
                default -> new Body("minecraft:husk", 1.0f);
            };
        };
    }
}
