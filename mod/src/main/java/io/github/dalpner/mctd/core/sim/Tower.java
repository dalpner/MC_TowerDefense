package io.github.dalpner.mctd.core.sim;

import io.github.dalpner.mctd.core.TargetMode;
import io.github.dalpner.mctd.core.TowerType;

/** A placed tower. Positions are arena cells; the tower shoots from the centre of its cell. */
public final class Tower {
    private final int id;
    private final TowerType type;
    private final int cellX;
    private final int cellZ;
    int tier = 1;
    TargetMode mode = TargetMode.FIRST;
    double cooldown;
    int invested;
    int builtPhase;
    int kills;
    double damageDealt;
    Creep rampTarget;
    int rampStacks;
    double buffDamage;
    double buffSpeed;

    Tower(int id, TowerType type, int cellX, int cellZ) {
        this.id = id;
        this.type = type;
        this.cellX = cellX;
        this.cellZ = cellZ;
    }

    public int id() {
        return id;
    }

    public TowerType type() {
        return type;
    }

    public int cellX() {
        return cellX;
    }

    public int cellZ() {
        return cellZ;
    }

    public double x() {
        return cellX + 0.5;
    }

    public double z() {
        return cellZ + 0.5;
    }

    public int tier() {
        return tier;
    }

    public TowerType.Tier stats() {
        return type.tier(tier);
    }

    public TargetMode mode() {
        return mode;
    }

    public int invested() {
        return invested;
    }

    public int kills() {
        return kills;
    }

    public double damageDealt() {
        return damageDealt;
    }

    /** Damage bonus from support towers nearby (0.2 = +20%). */
    public double buffDamage() {
        return buffDamage;
    }

    /** Attack speed bonus from support towers nearby. */
    public double buffSpeed() {
        return buffSpeed;
    }

    public int rampStacks() {
        return rampStacks;
    }

    public boolean maxed() {
        return tier >= type.maxTier();
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    long key() {
        return key(cellX, cellZ);
    }

    @Override
    public String toString() {
        return type.id() + "@" + cellX + "," + cellZ + " T" + tier;
    }
}
