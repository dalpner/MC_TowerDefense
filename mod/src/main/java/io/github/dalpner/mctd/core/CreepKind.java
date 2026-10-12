package io.github.dalpner.mctd.core;

/**
 * Wave archetypes. Each kind scales the base hit points of a wave and defines how many creeps
 * appear, how fast they walk and what they are worth.
 */
public enum CreepKind {
    //           id            hp    count speed interval bounty lives
    NORMAL("normal", 1.0, 10, 3.0, 18, 1.0, 1),
    FAST("fast", 0.7, 10, 4.6, 12, 1.0, 1),
    MASS("mass", 0.35, 24, 3.2, 7, 0.4, 1),
    AIR("air", 0.9, 8, 3.0, 22, 1.2, 1),
    BOSS("boss", 10.0, 1, 1.6, 0, 12.0, 5),
    FINAL_BOSS("final_boss", 14.0, 1, 1.2, 0, 40.0, 20),
    ELEMENTAL("elemental", 3.0, 1, 2.4, 0, 6.0, 1);

    private final String id;
    private final double hpMultiplier;
    private final int count;
    private final double speed;
    private final int spawnInterval;
    private final double bountyMultiplier;
    private final int livesCost;

    CreepKind(String id, double hpMultiplier, int count, double speed, int spawnInterval,
              double bountyMultiplier, int livesCost) {
        this.id = id;
        this.hpMultiplier = hpMultiplier;
        this.count = count;
        this.speed = speed;
        this.spawnInterval = spawnInterval;
        this.bountyMultiplier = bountyMultiplier;
        this.livesCost = livesCost;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mctd.creep_kind." + id;
    }

    public double hpMultiplier() {
        return hpMultiplier;
    }

    public int count() {
        return count;
    }

    /** Walking speed in blocks per second. */
    public double speed() {
        return speed;
    }

    /** Ticks between two spawns of the same wave. */
    public int spawnInterval() {
        return spawnInterval;
    }

    public double bountyMultiplier() {
        return bountyMultiplier;
    }

    public int livesCost() {
        return livesCost;
    }

    public boolean flying() {
        return this == AIR;
    }

    /** Boss-like creeps cannot be stunned or knocked back and are only half as affected by slows. */
    public boolean bossLike() {
        return this == BOSS || this == FINAL_BOSS || this == ELEMENTAL;
    }

    public static CreepKind byId(String id) {
        for (CreepKind k : values()) {
            if (k.id.equals(id)) {
                return k;
            }
        }
        return null;
    }
}
