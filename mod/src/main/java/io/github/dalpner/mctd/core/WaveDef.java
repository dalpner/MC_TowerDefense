package io.github.dalpner.mctd.core;

/**
 * A complete wave: {@code count} creeps of the given spec, spawned {@code spawnInterval} ticks apart.
 */
public record WaveDef(int number, CreepSpec spec, int count, int spawnInterval) {

    public Element element() {
        return spec.element();
    }

    public CreepKind kind() {
        return spec.kind();
    }

    public double totalHp() {
        return spec.hp() * count;
    }
}
