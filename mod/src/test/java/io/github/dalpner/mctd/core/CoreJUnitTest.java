package io.github.dalpner.mctd.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dalpner.mctd.core.sim.GameConfig;
import org.junit.jupiter.api.Test;

/** Runs the framework-free core checks and a few full bot games during the Gradle build. */
class CoreJUnitTest {

    @Test
    void selfTest() {
        CoreSelfTest.runAll();
    }

    @Test
    void easyGameIsWinnableByTheBot() {
        BalanceSim.Result result = BalanceSim.play(GameConfig.of(Difficulty.EASY, ArenaLayout.SERPENTINE, 7920),
                BalanceSim.PLANS[1]);
        assertTrue(result.victory(), "bot should win on easy: " + BalanceSim.format(result, 1, true));
        assertEquals(50, result.lastWave());
    }

    @Test
    void hardGameIsHarderThanEasy() {
        BalanceSim.Result easy = BalanceSim.play(GameConfig.of(Difficulty.EASY, ArenaLayout.ZIGZAG, 11),
                BalanceSim.PLANS[0]);
        BalanceSim.Result hard = BalanceSim.play(GameConfig.of(Difficulty.HARD, ArenaLayout.ZIGZAG, 11),
                BalanceSim.PLANS[0]);
        assertTrue(easy.livesLeft() * 1.0 / Difficulty.EASY.lives() > hard.livesLeft() * 1.0 / Difficulty.HARD.lives()
                || easy.lastWave() > hard.lastWave(), "hard must be harder than easy");
    }
}
