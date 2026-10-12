package io.github.dalpner.mctd.core.sim;

import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.WaveDef;

import java.util.List;

/** Receives everything that happens in a {@link Game}, e.g. to show it in the world. */
public interface GameListener {
    default void creepSpawned(Creep creep) {
    }

    default void creepDamaged(Creep creep, Tower source, double amount, boolean crit) {
    }

    /** Final death of a creep (gold already granted, see {@link Creep#goldReward()}). */
    default void creepDied(Creep creep, Tower killer) {
    }

    default void creepLeaked(Creep creep) {
    }

    /** An undead creep fell and will rise again shortly. */
    default void creepFell(Creep creep) {
    }

    /** An undead creep rose again. */
    default void creepRose(Creep creep) {
    }

    default void creepsHealed(Creep healer, List<Creep> healed) {
    }

    /** A tower attacked; for chains the targets are in jump order, for splashes the primary target comes first. */
    default void towerAttacked(Tower tower, List<Creep> targets) {
    }

    default void waveStarted(WaveDef wave) {
    }

    default void waveCleared(int wave, int bonus, int interest, int towerGold) {
    }

    default void countdownStarted(int nextWave, int ticks) {
    }

    default void essenceGained(int essences) {
    }

    default void elementBought(Element element, int level, Creep guardian) {
    }

    default void gameOver(boolean victory) {
    }
}
