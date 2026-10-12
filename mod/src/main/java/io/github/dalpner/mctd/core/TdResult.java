package io.github.dalpner.mctd.core;

/** Outcome of a player action on the {@link io.github.dalpner.mctd.core.sim.Game}. */
public enum TdResult {
    OK("ok"),
    NOT_ENOUGH_GOLD("not_enough_gold"),
    LOCKED("locked"),
    MAX_TIER("max_tier"),
    OCCUPIED("occupied"),
    NOT_BUILDABLE("not_buildable"),
    NO_TOWER("no_tower"),
    NO_ESSENCE("no_essence"),
    ELEMENT_MAXED("element_maxed"),
    GAME_OVER("game_over"),
    NO_MORE_WAVES("no_more_waves"),
    STILL_SPAWNING("still_spawning");

    private final String id;

    TdResult(String id) {
        this.id = id;
    }

    public boolean ok() {
        return this == OK;
    }

    public String translationKey() {
        return "mctd.result." + id;
    }
}
