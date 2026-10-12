package io.github.dalpner.mctd.core.sim;

import io.github.dalpner.mctd.core.Ability;
import io.github.dalpner.mctd.core.CreepSpec;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.PathLine;

/** A creep walking along the path. All fields are managed by {@link Game}. */
public final class Creep {
    private final int id;
    private final CreepSpec spec;
    final double maxHp;
    double hp;

    // position
    double distance;
    double lateral;
    double x;
    double z;
    double facingX = 1;
    double facingZ;

    // life cycle
    boolean removed;
    boolean leaked;
    boolean risen;
    int reviveTicks;
    int goldReward;

    // status effects
    double slow;
    int slowTicks;
    int stunTicks;
    double burnDps;
    int burnTicks;
    Tower burnSource;
    double poisonDps;
    int poisonStacks;
    int poisonTicks;
    Tower poisonSource;
    double diseaseDps;
    int diseaseTicks;
    double diseaseRadius;
    Tower diseaseSource;
    double curse;
    int curseTicks;
    double knockbackFactor = 1.0;
    int healTimer;

    Creep(int id, CreepSpec spec) {
        this.id = id;
        this.spec = spec;
        this.maxHp = spec.hp();
        this.hp = spec.hp();
        this.healTimer = 60;
    }

    void updatePosition(PathLine path, double[] tmp) {
        path.sample(distance, lateral, tmp);
        x = tmp[0];
        z = tmp[1];
        facingX = tmp[2];
        facingZ = tmp[3];
    }

    void clearEffects() {
        slow = 0;
        slowTicks = 0;
        stunTicks = 0;
        burnTicks = 0;
        burnDps = 0;
        poisonTicks = 0;
        poisonStacks = 0;
        poisonDps = 0;
        diseaseTicks = 0;
        diseaseDps = 0;
        curse = 0;
        curseTicks = 0;
    }

    public int id() {
        return id;
    }

    public CreepSpec spec() {
        return spec;
    }

    public Element element() {
        return spec.element();
    }

    public boolean has(Ability ability) {
        return spec.has(ability);
    }

    public boolean flying() {
        return spec.flying();
    }

    public double hp() {
        return hp;
    }

    public double maxHp() {
        return maxHp;
    }

    public double hpFraction() {
        return maxHp <= 0 ? 0 : Math.max(0, Math.min(1, hp / maxHp));
    }

    public double distance() {
        return distance;
    }

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }

    public double facingX() {
        return facingX;
    }

    public double facingZ() {
        return facingZ;
    }

    public boolean removed() {
        return removed;
    }

    public boolean leaked() {
        return leaked;
    }

    /** Undead creep lying on the ground before rising again. */
    public boolean reviving() {
        return reviveTicks > 0;
    }

    public boolean targetable() {
        return !removed && reviveTicks <= 0;
    }

    public int goldReward() {
        return goldReward;
    }

    public boolean slowed() {
        return slowTicks > 0 && slow > 0;
    }

    public boolean stunned() {
        return stunTicks > 0;
    }

    public boolean burning() {
        return burnTicks > 0;
    }

    public boolean poisoned() {
        return poisonTicks > 0 && poisonStacks > 0;
    }

    public boolean diseased() {
        return diseaseTicks > 0;
    }

    public boolean cursed() {
        return curseTicks > 0 && curse > 0;
    }

    @Override
    public String toString() {
        return "Creep#" + id + "[" + spec.kind().id() + " " + (spec.element() == null ? "neutral" : spec.element().id())
                + " hp=" + Math.round(hp) + "/" + Math.round(maxHp) + " d=" + Math.round(distance) + "]";
    }
}
