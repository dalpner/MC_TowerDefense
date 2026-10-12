package io.github.dalpner.mctd.core;

/** Special creep abilities that appear in later waves. */
public enum Ability {
    /** Cannot be slowed, stunned or knocked back. */
    IMMUNE("immune"),
    /** Regenerates a share of its maximum hit points every second. */
    REGEN("regen"),
    /** Periodically heals itself and nearby creeps. */
    HEALER("healer"),
    /** Rises again once after being killed. */
    UNDEAD("undead"),
    /** Splits into two smaller creeps when killed. */
    SPLIT("split"),
    /** Every tower hit is reduced by a flat armor value. */
    ARMORED("armored");

    private final String id;

    Ability(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mctd.ability." + id;
    }

    public static Ability byId(String id) {
        for (Ability a : values()) {
            if (a.id.equals(id)) {
                return a;
            }
        }
        return null;
    }
}
