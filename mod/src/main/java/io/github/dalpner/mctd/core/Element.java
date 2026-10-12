package io.github.dalpner.mctd.core;

/**
 * The six elements, arranged on a wheel like in Element TD. Every element deals double damage to the
 * element after it and half damage to the element before it:
 * Light → Darkness → Water → Fire → Nature → Earth → Light.
 * Towers without an element (and the final boss) are neutral and always use a multiplier of 1.
 */
public enum Element {
    LIGHT("light", 0xFFE873, 'e'),
    DARKNESS("darkness", 0x9B4DFF, '5'),
    WATER("water", 0x3D8BFF, '9'),
    FIRE("fire", 0xFF5522, 'c'),
    NATURE("nature", 0x55DD33, 'a'),
    EARTH("earth", 0xC08040, '6');

    public static final double STRONG = 2.0;
    public static final double WEAK = 0.5;
    public static final int MAX_LEVEL = 3;

    private static final Element[] VALUES = values();

    private final String id;
    private final int color;
    private final char formattingCode;

    Element(String id, int color, char formattingCode) {
        this.id = id;
        this.color = color;
        this.formattingCode = formattingCode;
    }

    public String id() {
        return id;
    }

    /** RGB color used for particles and HUD elements. */
    public int color() {
        return color;
    }

    /** Minecraft chat formatting code (e.g. 'c' for red). */
    public char formattingCode() {
        return formattingCode;
    }

    public String translationKey() {
        return "mctd.element." + id;
    }

    /** The element this one deals double damage to. */
    public Element beats() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /** The element that deals double damage to this one. */
    public Element beatenBy() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }

    /** Damage multiplier of an attack with element {@code attacker} against a creep of element {@code defender}. */
    public static double multiplier(Element attacker, Element defender) {
        if (attacker == null || defender == null) {
            return 1.0;
        }
        if (attacker.beats() == defender) {
            return STRONG;
        }
        if (defender.beats() == attacker) {
            return WEAK;
        }
        return 1.0;
    }

    public static Element byId(String id) {
        for (Element e : VALUES) {
            if (e.id.equals(id)) {
                return e;
            }
        }
        return null;
    }
}
