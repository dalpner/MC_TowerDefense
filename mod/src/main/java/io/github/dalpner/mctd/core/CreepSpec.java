package io.github.dalpner.mctd.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Everything needed to spawn one creep.
 *
 * @param wave       wave number the creep belongs to; 0 for element guardians
 * @param element    element of the creep, {@code null} for the neutral final boss
 * @param mob        vanilla entity id used as the creep's body
 * @param scale      size multiplier for the body
 * @param armor      flat damage reduction per tower hit
 * @param splitChild whether this creep was created by a splitting parent
 */
public record CreepSpec(int wave, Element element, CreepKind kind, Set<Ability> abilities,
                        double hp, double speed, int bounty, int livesCost,
                        String mob, float scale, double armor, boolean splitChild) {

    public CreepSpec {
        abilities = abilities.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(abilities));
    }

    public boolean has(Ability ability) {
        return abilities.contains(ability);
    }

    public boolean flying() {
        return kind.flying();
    }

    public boolean bossLike() {
        return kind.bossLike();
    }

    /** The smaller creep a splitting creep breaks into. */
    public CreepSpec toSplitChild() {
        return new CreepSpec(wave, element, kind, abilities, hp * 0.3, speed * 1.1,
                Math.max(1, bounty / 3), livesCost, mob, scale * 0.7f, armor * 0.5, true);
    }
}
