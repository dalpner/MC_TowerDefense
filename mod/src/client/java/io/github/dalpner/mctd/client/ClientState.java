package io.github.dalpner.mctd.client;

import io.github.dalpner.mctd.net.HudPayload;

/** The latest game state received from the server. */
public final class ClientState {
    private static HudPayload hud = HudPayload.inactive();
    private static long receivedAt;

    private ClientState() {
    }

    static void update(HudPayload payload) {
        hud = payload;
        receivedAt = System.currentTimeMillis();
    }

    static void reset() {
        hud = HudPayload.inactive();
    }

    public static HudPayload hud() {
        return hud;
    }

    /** Countdown in ticks, extrapolated since the last update so the HUD counts down smoothly. */
    public static int countdown() {
        if (hud.phase() != HudPayload.PHASE_BUILD || hud.paused()) {
            return hud.countdown();
        }
        long elapsedTicks = (System.currentTimeMillis() - receivedAt) / 50;
        return (int) Math.max(0, hud.countdown() - elapsedTicks);
    }
}
