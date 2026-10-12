package io.github.dalpner.mctd.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static definition of a tower: its elements, attack style and the stats of each of its three tiers.
 * Towers are created with {@link Builder}; every call to {@link Builder#tier} starts a new tier that
 * inherits all effect values of the previous one, so only changes have to be written down.
 */
public final class TowerType {

    public enum Category {
        BASIC("basic"), ELEMENT("element"), DUAL("dual"), TRIPLE("triple");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "mctd.category." + id;
        }
    }

    /** Stats of one tier. All durations are in ticks (20 per second), distances in blocks. */
    public static final class Tier {
        /** Gold to build (tier 1) or to upgrade to this tier. */
        public int cost;
        public double damage;
        public double range;
        public int cooldown;
        /** MULTI: number of targets. CHAIN: number of extra jumps. */
        public int targets = 1;
        public double splashRadius;
        public double splashFactor;
        public double chainDecay = 0.8;
        public double chainRange = 4.0;
        public double slow;
        public int slowTicks;
        public double stunChance;
        public int stunTicks;
        /** Burn damage per second as a share of the hit damage. */
        public double burn;
        public int burnTicks;
        /** Poison damage per second and stack as a share of the hit damage. */
        public double poison;
        public int poisonTicks;
        public int poisonStacks;
        /** Extra damage taken by cursed creeps (0.2 = +20%). */
        public double curse;
        public int curseTicks;
        /** Blocks a creep is pushed back along the path. */
        public double knockback;
        /** Non-boss creeps below this share of their hit points die instantly. */
        public double execute;
        /** Bonus damage per hit as a share of the target's maximum hit points. */
        public double percentHp;
        public double critChance;
        public double critMultiplier = 1.0;
        /** RAMP: damage bonus per consecutive hit, and the maximum bonus. */
        public double ramp;
        public double rampMax;
        /** Damage bonus at maximum range (scales linearly with distance). */
        public double distanceBonus;
        /** Disease damage per second as a share of the hit damage; spreads to nearby creeps. */
        public double disease;
        public int diseaseTicks;
        public double diseaseRadius;
        /** Support aura: bonus damage / attack speed for other towers within range. */
        public double auraDamage;
        public double auraSpeed;
        /** Extra bounty for creeps that die within range. */
        public double bountyBonus;
        /** Gold granted whenever a wave is cleared. */
        public int waveGold;

        Tier copy() {
            Tier t = new Tier();
            t.cost = cost;
            t.damage = damage;
            t.range = range;
            t.cooldown = cooldown;
            t.targets = targets;
            t.splashRadius = splashRadius;
            t.splashFactor = splashFactor;
            t.chainDecay = chainDecay;
            t.chainRange = chainRange;
            t.slow = slow;
            t.slowTicks = slowTicks;
            t.stunChance = stunChance;
            t.stunTicks = stunTicks;
            t.burn = burn;
            t.burnTicks = burnTicks;
            t.poison = poison;
            t.poisonTicks = poisonTicks;
            t.poisonStacks = poisonStacks;
            t.curse = curse;
            t.curseTicks = curseTicks;
            t.knockback = knockback;
            t.execute = execute;
            t.percentHp = percentHp;
            t.critChance = critChance;
            t.critMultiplier = critMultiplier;
            t.ramp = ramp;
            t.rampMax = rampMax;
            t.distanceBonus = distanceBonus;
            t.disease = disease;
            t.diseaseTicks = diseaseTicks;
            t.diseaseRadius = diseaseRadius;
            t.auraDamage = auraDamage;
            t.auraSpeed = auraSpeed;
            t.bountyBonus = bountyBonus;
            t.waveGold = waveGold;
            return t;
        }

        /** Raw single-target damage per second. */
        public double dps() {
            return cooldown <= 0 ? 0 : damage * 20.0 / cooldown;
        }

        public double attacksPerSecond() {
            return cooldown <= 0 ? 0 : 20.0 / cooldown;
        }

        public boolean hasAura() {
            return auraDamage > 0 || auraSpeed > 0;
        }
    }

    private final String id;
    private final Category category;
    private final List<Element> elements;
    private final Element damageElement;
    private final boolean adaptive;
    private final AttackStyle style;
    private final boolean hitsAir;
    private final boolean hitsGround;
    private final List<Tier> tiers;

    private TowerType(Builder b) {
        this.id = b.id;
        this.category = b.category;
        this.elements = List.copyOf(b.elements);
        this.damageElement = b.damageElement;
        this.adaptive = b.adaptive;
        this.style = b.style;
        this.hitsAir = b.hitsAir;
        this.hitsGround = b.hitsGround;
        this.tiers = Collections.unmodifiableList(new ArrayList<>(b.tiers));
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mctd.tower." + id;
    }

    public String descriptionKey() {
        return "mctd.tower." + id + ".desc";
    }

    public Category category() {
        return category;
    }

    public List<Element> elements() {
        return elements;
    }

    /** Element of the damage dealt; {@code null} means neutral. Adaptive towers pick their best element per target. */
    public Element damageElement() {
        return damageElement;
    }

    public boolean adaptive() {
        return adaptive;
    }

    public AttackStyle style() {
        return style;
    }

    public boolean hitsAir() {
        return hitsAir;
    }

    public boolean hitsGround() {
        return hitsGround;
    }

    public int maxTier() {
        return tiers.size();
    }

    /** Stats of a tier (1-based). */
    public Tier tier(int tier) {
        return tiers.get(tier - 1);
    }

    /** Gold invested into a tower of this type once it reached the given tier. */
    public int totalCost(int tier) {
        int sum = 0;
        for (int i = 1; i <= tier; i++) {
            sum += tier(i).cost;
        }
        return sum;
    }

    /** Whether the element levels allow building / upgrading to the given tier. */
    public boolean unlocked(int[] elementLevels, int tier) {
        for (Element e : elements) {
            if (elementLevels[e.ordinal()] < tier) {
                return false;
            }
        }
        return true;
    }

    /** The element used against a creep of the given element. */
    public Element damageElementAgainst(Element creepElement) {
        if (!adaptive || creepElement == null) {
            return damageElement;
        }
        Element best = damageElement;
        double bestMult = Element.multiplier(best, creepElement);
        for (Element e : elements) {
            double m = Element.multiplier(e, creepElement);
            if (m > bestMult) {
                best = e;
                bestMult = m;
            }
        }
        return best;
    }

    public boolean isSupportOnly() {
        return style == AttackStyle.SUPPORT;
    }

    @Override
    public String toString() {
        return id;
    }

    public static Builder builder(String id, Category category, Element... elements) {
        return new Builder(id, category, elements);
    }

    public static final class Builder {
        private final String id;
        private final Category category;
        private final List<Element> elements = new ArrayList<>();
        private Element damageElement;
        private boolean adaptive;
        private AttackStyle style = AttackStyle.SINGLE;
        private boolean hitsAir = true;
        private boolean hitsGround = true;
        private final List<Tier> tiers = new ArrayList<>();
        private Tier current;

        private Builder(String id, Category category, Element... elements) {
            this.id = id;
            this.category = category;
            Collections.addAll(this.elements, elements);
            this.damageElement = elements.length > 0 ? elements[0] : null;
        }

        public Builder damageElement(Element element) {
            this.damageElement = element;
            return this;
        }

        /** Deals the most effective of its elements against every target. */
        public Builder adaptive() {
            this.adaptive = true;
            return this;
        }

        public Builder style(AttackStyle style) {
            this.style = style;
            return this;
        }

        public Builder groundOnly() {
            this.hitsAir = false;
            return this;
        }

        /** Starts a new tier, inheriting all effect values of the previous tier. */
        public Builder tier(int cost, double damage, double range, int cooldown) {
            current = current == null ? new Tier() : current.copy();
            current.cost = cost;
            current.damage = damage;
            current.range = range;
            current.cooldown = cooldown;
            tiers.add(current);
            return this;
        }

        public Builder targets(int targets) {
            current.targets = targets;
            return this;
        }

        public Builder splash(double radius, double factor) {
            current.splashRadius = radius;
            current.splashFactor = factor;
            return this;
        }

        public Builder chain(int jumps, double decay, double range) {
            current.targets = jumps;
            current.chainDecay = decay;
            current.chainRange = range;
            return this;
        }

        public Builder slow(double amount, int ticks) {
            current.slow = amount;
            current.slowTicks = ticks;
            return this;
        }

        public Builder stun(double chance, int ticks) {
            current.stunChance = chance;
            current.stunTicks = ticks;
            return this;
        }

        public Builder burn(double share, int ticks) {
            current.burn = share;
            current.burnTicks = ticks;
            return this;
        }

        public Builder poison(double share, int ticks, int maxStacks) {
            current.poison = share;
            current.poisonTicks = ticks;
            current.poisonStacks = maxStacks;
            return this;
        }

        public Builder curse(double amount, int ticks) {
            current.curse = amount;
            current.curseTicks = ticks;
            return this;
        }

        public Builder knockback(double blocks) {
            current.knockback = blocks;
            return this;
        }

        public Builder execute(double threshold) {
            current.execute = threshold;
            return this;
        }

        public Builder percentHp(double share) {
            current.percentHp = share;
            return this;
        }

        public Builder crit(double chance, double multiplier) {
            current.critChance = chance;
            current.critMultiplier = multiplier;
            return this;
        }

        public Builder ramp(double perHit, double max) {
            current.ramp = perHit;
            current.rampMax = max;
            return this;
        }

        public Builder distanceBonus(double bonus) {
            current.distanceBonus = bonus;
            return this;
        }

        public Builder disease(double share, int ticks, double radius) {
            current.disease = share;
            current.diseaseTicks = ticks;
            current.diseaseRadius = radius;
            return this;
        }

        public Builder aura(double damage, double speed) {
            current.auraDamage = damage;
            current.auraSpeed = speed;
            return this;
        }

        public Builder bounty(double bonus, int waveGold) {
            current.bountyBonus = bonus;
            current.waveGold = waveGold;
            return this;
        }

        public TowerType build() {
            if (tiers.isEmpty()) {
                throw new IllegalStateException("Tower " + id + " has no tiers");
            }
            return new TowerType(this);
        }
    }
}
