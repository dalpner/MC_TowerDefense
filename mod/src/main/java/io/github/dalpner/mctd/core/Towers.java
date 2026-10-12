package io.github.dalpner.mctd.core;

import static io.github.dalpner.mctd.core.AttackStyle.*;
import static io.github.dalpner.mctd.core.Element.*;
import static io.github.dalpner.mctd.core.TowerType.Category.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All towers of the game: 2 basic towers, 6 element towers, 15 dual and 20 triple element towers.
 * Tier N of an element tower requires level N in each of its elements.
 */
public final class Towers {
    private static final Map<String, TowerType> BY_ID = new LinkedHashMap<>();
    private static final List<TowerType> ALL;

    // ---- Basic towers (no element, neutral damage) ----
    public static final TowerType ARROW = add(TowerType.builder("arrow", BASIC)
            .tier(40, 11, 6.0, 15)
            .tier(90, 38, 6.5, 15)
            .tier(220, 113, 7.0, 15));

    public static final TowerType CANNON = add(TowerType.builder("cannon", BASIC)
            .style(SPLASH).groundOnly()
            .tier(60, 17, 5.5, 30).splash(2.0, 0.6)
            .tier(130, 58, 6.0, 30).splash(2.2, 0.6)
            .tier(320, 165, 6.5, 30).splash(2.5, 0.6));

    // ---- Element towers ----
    public static final TowerType LIGHT_TOWER = add(TowerType.builder("light", ELEMENT, LIGHT)
            .style(CHAIN)
            .tier(120, 33, 6.5, 18).chain(1, 0.6, 3.5)
            .tier(280, 100, 7.0, 18).chain(2, 0.6, 3.5)
            .tier(700, 287, 7.5, 18).chain(3, 0.6, 3.5));

    public static final TowerType DARKNESS_TOWER = add(TowerType.builder("darkness", ELEMENT, DARKNESS)
            .tier(120, 91, 6.5, 40).curse(0.12, 60)
            .tier(280, 328, 6.5, 40).curse(0.16, 60)
            .tier(700, 1020, 7.0, 40).curse(0.20, 60));

    public static final TowerType WATER_TOWER = add(TowerType.builder("water", ELEMENT, WATER)
            .style(SPLASH)
            .tier(120, 23, 6.0, 20).splash(1.5, 0.5).slow(0.18, 40)
            .tier(280, 82, 6.5, 20).splash(1.6, 0.5).slow(0.24, 40)
            .tier(700, 244, 7.0, 20).splash(1.8, 0.5).slow(0.30, 50));

    public static final TowerType FIRE_TOWER = add(TowerType.builder("fire", ELEMENT, FIRE)
            .style(SPLASH)
            .tier(120, 20, 6.0, 25).splash(2.0, 0.6).burn(0.3, 60)
            .tier(280, 73, 6.5, 25)
            .tier(700, 225, 7.0, 25).splash(2.2, 0.6));

    public static final TowerType NATURE_TOWER = add(TowerType.builder("nature", ELEMENT, NATURE)
            .style(MULTI)
            .tier(120, 17, 6.5, 16).targets(2).poison(0.15, 80, 5)
            .tier(280, 64, 7.0, 16)
            .tier(700, 136, 7.5, 16).targets(3));

    public static final TowerType EARTH_TOWER = add(TowerType.builder("earth", ELEMENT, EARTH)
            .style(SPLASH).groundOnly()
            .tier(120, 43, 5.5, 40).splash(2.5, 0.7).stun(0.12, 12)
            .tier(280, 157, 6.0, 40).stun(0.16, 12)
            .tier(700, 491, 6.5, 40).stun(0.20, 14));

    // ---- Dual element towers ----
    public static final TowerType TWILIGHT = add(TowerType.builder("twilight", DUAL, LIGHT, DARKNESS)
            .adaptive()
            .tier(350, 206, 7.0, 30).percentHp(0.015)
            .tier(800, 713, 7.0, 30).percentHp(0.02)
            .tier(2000, 2130, 7.5, 30).percentHp(0.025));

    public static final TowerType ICE = add(TowerType.builder("ice", DUAL, LIGHT, WATER)
            .damageElement(WATER).style(SPLASH)
            .tier(350, 58, 6.5, 25).splash(2.0, 0.6).slow(0.35, 50).stun(0.08, 15)
            .tier(800, 203, 7.0, 25).slow(0.40, 50).stun(0.10, 15)
            .tier(2000, 616, 7.5, 25).slow(0.45, 60).stun(0.12, 18));

    public static final TowerType LIGHTNING = add(TowerType.builder("lightning", DUAL, LIGHT, FIRE)
            .style(CHAIN)
            .tier(350, 67, 7.0, 25).chain(4, 0.85, 4.0)
            .tier(800, 245, 7.5, 25).chain(5, 0.85, 4.0)
            .tier(2000, 775, 8.0, 25).chain(6, 0.85, 4.5));

    public static final TowerType SUNSPEAR = add(TowerType.builder("sunspear", DUAL, LIGHT, NATURE)
            .tier(350, 211, 11.0, 35).distanceBonus(0.8)
            .tier(800, 772, 12.0, 35)
            .tier(2000, 2440, 13.0, 35));

    public static final TowerType GOLD = add(TowerType.builder("gold", DUAL, LIGHT, EARTH)
            .damageElement(EARTH)
            .tier(350, 116, 6.5, 25).bounty(0.20, 10)
            .tier(800, 425, 7.0, 25).bounty(0.30, 25)
            .tier(2000, 1340, 7.5, 25).bounty(0.40, 60));

    public static final TowerType TIDAL = add(TowerType.builder("tidal", DUAL, DARKNESS, WATER)
            .damageElement(WATER).style(SPLASH)
            .tier(350, 111, 6.5, 35).splash(2.0, 0.5).knockback(1.5)
            .tier(800, 405, 7.0, 35).knockback(2.0)
            .tier(2000, 1280, 7.5, 35).knockback(2.5));

    public static final TowerType HELLFIRE = add(TowerType.builder("hellfire", DUAL, DARKNESS, FIRE)
            .damageElement(FIRE).style(RAMP)
            .tier(350, 28, 6.5, 8).ramp(0.12, 3.0)
            .tier(800, 101, 7.0, 8)
            .tier(2000, 320, 7.5, 8));

    public static final TowerType PLAGUE = add(TowerType.builder("plague", DUAL, DARKNESS, NATURE)
            .damageElement(NATURE)
            .tier(350, 94, 6.5, 25).disease(0.5, 100, 2.5)
            .tier(800, 343, 7.0, 25)
            .tier(2000, 1090, 7.5, 25).disease(0.5, 120, 3.0));

    public static final TowerType JINX = add(TowerType.builder("jinx", DUAL, DARKNESS, EARTH)
            .damageElement(DARKNESS).style(PULSE)
            .tier(350, 44, 5.5, 30).curse(0.20, 80)
            .tier(800, 154, 6.0, 30).curse(0.25, 80)
            .tier(2000, 473, 6.5, 30).curse(0.30, 80));

    public static final TowerType STEAM = add(TowerType.builder("steam", DUAL, WATER, FIRE)
            .damageElement(FIRE).style(PULSE)
            .tier(350, 56, 5.0, 20)
            .tier(800, 206, 5.5, 20)
            .tier(2000, 651, 6.0, 20));

    public static final TowerType SPRING = add(TowerType.builder("spring", DUAL, WATER, NATURE)
            .style(SUPPORT)
            .tier(350, 0, 5.5, 0).aura(0, 0.15)
            .tier(800, 0, 6.0, 0).aura(0, 0.22)
            .tier(2000, 0, 6.5, 0).aura(0, 0.30));

    public static final TowerType MUD = add(TowerType.builder("mud", DUAL, WATER, EARTH)
            .damageElement(EARTH).style(PULSE).groundOnly()
            .tier(350, 29, 5.0, 20).slow(0.30, 30)
            .tier(800, 104, 5.5, 20).slow(0.35, 30)
            .tier(2000, 319, 6.0, 20).slow(0.40, 30));

    public static final TowerType FORGE = add(TowerType.builder("forge", DUAL, FIRE, NATURE)
            .style(SUPPORT)
            .tier(350, 0, 5.5, 0).aura(0.15, 0)
            .tier(800, 0, 6.0, 0).aura(0.22, 0)
            .tier(2000, 0, 6.5, 0).aura(0.30, 0));

    public static final TowerType MAGMA = add(TowerType.builder("magma", DUAL, FIRE, EARTH)
            .style(SPLASH).groundOnly()
            .tier(350, 92, 6.0, 40).splash(2.5, 0.75).burn(0.4, 60)
            .tier(800, 338, 6.5, 40)
            .tier(2000, 1030, 7.0, 40).splash(2.8, 0.75));

    public static final TowerType QUAKE = add(TowerType.builder("quake", DUAL, NATURE, EARTH)
            .damageElement(EARTH).style(PULSE).groundOnly()
            .tier(350, 101, 5.5, 45).stun(0.35, 15)
            .tier(800, 363, 6.0, 45).stun(0.40, 15)
            .tier(2000, 1130, 6.5, 45).stun(0.45, 15));

    // ---- Triple element towers ----
    public static final TowerType MOON = add(TowerType.builder("moon", TRIPLE, LIGHT, DARKNESS, WATER)
            .damageElement(WATER).style(SPLASH)
            .tier(1000, 232, 7.0, 30).splash(2.5, 0.7).curse(0.25, 60).slow(0.25, 40)
            .tier(2200, 786, 7.5, 30).curse(0.30, 60).slow(0.30, 40)
            .tier(5500, 2350, 8.0, 30).curse(0.35, 60).slow(0.35, 40));

    public static final TowerType ECLIPSE = add(TowerType.builder("eclipse", TRIPLE, LIGHT, DARKNESS, FIRE)
            .adaptive()
            .tier(1000, 594, 7.5, 30).percentHp(0.03)
            .tier(2200, 2040, 8.0, 30).percentHp(0.035)
            .tier(5500, 6190, 8.5, 30).percentHp(0.04));

    public static final TowerType SOULTREE = add(TowerType.builder("soultree", TRIPLE, LIGHT, DARKNESS, NATURE)
            .damageElement(NATURE).style(MULTI)
            .tier(1000, 156, 7.0, 20).targets(4).execute(0.10)
            .tier(2200, 549, 7.5, 20).execute(0.12)
            .tier(5500, 1690, 8.0, 20).execute(0.15));

    public static final TowerType OBELISK = add(TowerType.builder("obelisk", TRIPLE, LIGHT, DARKNESS, EARTH)
            .damageElement(EARTH)
            .tier(1000, 431, 6.5, 30).aura(0.20, 0.10)
            .tier(2200, 1560, 7.0, 30).aura(0.25, 0.15)
            .tier(5500, 4950, 7.5, 30).aura(0.30, 0.20));

    public static final TowerType PRISM = add(TowerType.builder("prism", TRIPLE, LIGHT, WATER, FIRE)
            .style(CHAIN)
            .tier(1000, 206, 7.0, 25).chain(8, 0.9, 4.5)
            .tier(2200, 745, 7.5, 25)
            .tier(5500, 2370, 8.0, 25).chain(10, 0.9, 5.0));

    public static final TowerType RAINBOW = add(TowerType.builder("rainbow", TRIPLE, LIGHT, WATER, NATURE)
            .adaptive().style(MULTI)
            .tier(1000, 299, 7.0, 25).targets(3)
            .tier(2200, 1080, 7.5, 25)
            .tier(5500, 2580, 8.0, 25).targets(4));

    public static final TowerType CRYSTAL = add(TowerType.builder("crystal", TRIPLE, LIGHT, WATER, EARTH)
            .tier(1000, 442, 8.0, 25).crit(0.25, 3.5)
            .tier(2200, 1600, 8.5, 25)
            .tier(5500, 4720, 9.0, 25).crit(0.30, 3.5));

    public static final TowerType SUN_ALTAR = add(TowerType.builder("sun_altar", TRIPLE, LIGHT, FIRE, NATURE)
            .damageElement(FIRE).style(PULSE)
            .tier(1000, 198, 6.0, 30).burn(0.3, 60)
            .tier(2200, 715, 6.5, 30)
            .tier(5500, 2280, 7.0, 30));

    public static final TowerType METEOR = add(TowerType.builder("meteor", TRIPLE, LIGHT, FIRE, EARTH)
            .damageElement(FIRE).style(SPLASH).groundOnly()
            .tier(1000, 500, 8.0, 50).splash(3.0, 0.8).stun(0.25, 15)
            .tier(2200, 1810, 8.5, 50)
            .tier(5500, 5540, 9.0, 50).splash(3.3, 0.8));

    public static final TowerType TREASURY = add(TowerType.builder("treasury", TRIPLE, LIGHT, NATURE, EARTH)
            .damageElement(EARTH)
            .tier(1000, 395, 7.0, 25).bounty(0.50, 50)
            .tier(2200, 1430, 7.5, 25).bounty(0.65, 120)
            .tier(5500, 4540, 8.0, 25).bounty(0.80, 300));

    public static final TowerType MIST = add(TowerType.builder("mist", TRIPLE, DARKNESS, WATER, FIRE)
            .damageElement(DARKNESS).style(PULSE)
            .tier(1000, 166, 6.0, 25).curse(0.25, 60).slow(0.25, 40)
            .tier(2200, 600, 6.5, 25)
            .tier(5500, 1790, 7.0, 25).curse(0.30, 60).slow(0.30, 40));

    public static final TowerType SWAMP = add(TowerType.builder("swamp", TRIPLE, DARKNESS, WATER, NATURE)
            .damageElement(NATURE).style(PULSE)
            .tier(1000, 105, 6.0, 30).poison(0.25, 100, 8).slow(0.30, 40)
            .tier(2200, 380, 6.5, 30)
            .tier(5500, 1170, 7.0, 30).slow(0.35, 40));

    public static final TowerType ABYSS = add(TowerType.builder("abyss", TRIPLE, DARKNESS, WATER, EARTH)
            .damageElement(DARKNESS).style(SPLASH).groundOnly()
            .tier(1000, 420, 7.0, 40).splash(2.5, 0.7).knockback(2.5)
            .tier(2200, 1520, 7.5, 40).knockback(3.0)
            .tier(5500, 4830, 8.0, 40).knockback(3.5));

    public static final TowerType BLIGHT = add(TowerType.builder("blight", TRIPLE, DARKNESS, FIRE, NATURE)
            .damageElement(FIRE)
            .tier(1000, 198, 7.0, 25).disease(0.6, 120, 3.0).burn(0.3, 60)
            .tier(2200, 715, 7.5, 25)
            .tier(5500, 2280, 8.0, 25));

    public static final TowerType VOLCANO = add(TowerType.builder("volcano", TRIPLE, DARKNESS, FIRE, EARTH)
            .damageElement(FIRE).style(PULSE).groundOnly()
            .tier(1000, 281, 6.0, 40).burn(0.4, 60)
            .tier(2200, 1020, 6.5, 40)
            .tier(5500, 3240, 7.0, 40));

    public static final TowerType NECROPOLIS = add(TowerType.builder("necropolis", TRIPLE, DARKNESS, NATURE, EARTH)
            .damageElement(DARKNESS).style(MULTI)
            .tier(1000, 152, 6.5, 20).targets(5).execute(0.12)
            .tier(2200, 529, 7.0, 20).execute(0.15)
            .tier(5500, 1630, 7.5, 20).execute(0.18));

    public static final TowerType GEYSER = add(TowerType.builder("geyser", TRIPLE, WATER, FIRE, NATURE)
            .damageElement(WATER).style(SPLASH)
            .tier(1000, 267, 7.0, 35).splash(2.5, 0.7).knockback(2.0).slow(0.30, 40)
            .tier(2200, 966, 7.5, 35)
            .tier(5500, 3080, 8.0, 35).knockback(2.5));

    public static final TowerType LAVA_FLOW = add(TowerType.builder("lava_flow", TRIPLE, WATER, FIRE, EARTH)
            .damageElement(EARTH).style(SPLASH).groundOnly()
            .tier(1000, 212, 6.5, 35).splash(3.0, 0.8).slow(0.35, 40).burn(0.3, 60)
            .tier(2200, 767, 7.0, 35)
            .tier(5500, 2440, 7.5, 35));

    public static final TowerType PRIMEVAL = add(TowerType.builder("primeval", TRIPLE, WATER, NATURE, EARTH)
            .damageElement(NATURE).style(MULTI)
            .tier(1000, 104, 7.0, 20).targets(6).poison(0.20, 100, 6)
            .tier(2200, 377, 7.5, 20)
            .tier(5500, 1200, 8.0, 20));

    public static final TowerType WILDFIRE = add(TowerType.builder("wildfire", TRIPLE, FIRE, NATURE, EARTH)
            .style(CHAIN)
            .tier(1000, 134, 7.0, 25).chain(6, 0.88, 4.0).burn(0.4, 60)
            .tier(2200, 484, 7.5, 25)
            .tier(5500, 1540, 8.0, 25).chain(7, 0.88, 4.5));

    static {
        ALL = Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    private Towers() {
    }

    private static TowerType add(TowerType.Builder builder) {
        TowerType type = builder.build();
        if (BY_ID.put(type.id(), type) != null) {
            throw new IllegalStateException("Duplicate tower id " + type.id());
        }
        return type;
    }

    public static List<TowerType> all() {
        return ALL;
    }

    public static TowerType byId(String id) {
        return BY_ID.get(id);
    }

    public static List<TowerType> byCategory(TowerType.Category category) {
        List<TowerType> list = new ArrayList<>();
        for (TowerType t : ALL) {
            if (t.category() == category) {
                list.add(t);
            }
        }
        return list;
    }

    /** The tower whose element set is exactly the given set, or {@code null}. */
    public static TowerType forElements(Set<Element> elements) {
        for (TowerType t : ALL) {
            if (!t.elements().isEmpty() && t.elements().size() == elements.size()
                    && EnumSet.copyOf(t.elements()).equals(elements)) {
                return t;
            }
        }
        return null;
    }
}
