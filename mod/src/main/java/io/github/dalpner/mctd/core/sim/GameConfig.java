package io.github.dalpner.mctd.core.sim;

import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.core.WaveGenerator;

/**
 * Settings of a game.
 *
 * @param elementGuardians whether buying an element summons an element guardian
 */
public record GameConfig(Difficulty difficulty, ArenaLayout layout, long seed, int maxWaves,
                         boolean elementGuardians) {

    public static GameConfig of(Difficulty difficulty, ArenaLayout layout, long seed) {
        return new GameConfig(difficulty, layout, seed, WaveGenerator.DEFAULT_WAVES, true);
    }
}
