package io.github.dalpner.mctd.core;

public enum AttackStyle {
    /** One target per attack. */
    SINGLE,
    /** Several targets per attack, each takes full damage. */
    MULTI,
    /** Hits the target and damages creeps around it. */
    SPLASH,
    /** Hits the target and jumps to nearby creeps with decaying damage. */
    CHAIN,
    /** Damages every creep in range at once. */
    PULSE,
    /** Single target; damage rises with every consecutive hit on the same creep. */
    RAMP,
    /** Does not attack; only buffs nearby towers. */
    SUPPORT
}
