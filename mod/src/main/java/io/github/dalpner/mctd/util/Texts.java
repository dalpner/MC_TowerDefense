package io.github.dalpner.mctd.util;

import io.github.dalpner.mctd.core.Ability;
import io.github.dalpner.mctd.core.AttackStyle;
import io.github.dalpner.mctd.core.CreepKind;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.WaveDef;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Text helpers shared by menus, chat messages and the HUD. */
public final class Texts {
    private Texts() {
    }

    public static Formatting color(Element element) {
        if (element == null) {
            return Formatting.WHITE;
        }
        Formatting f = Formatting.byCode(element.formattingCode());
        return f == null ? Formatting.WHITE : f;
    }

    public static String symbol(Element element) {
        if (element == null) {
            return "✦";
        }
        return switch (element) {
            case LIGHT -> "☀";
            case DARKNESS -> "☾";
            case WATER -> "≈";
            case FIRE -> "♨";
            case NATURE -> "❀";
            case EARTH -> "▲";
        };
    }

    public static String symbol(Ability ability) {
        return switch (ability) {
            case IMMUNE -> "✪";
            case REGEN -> "✚";
            case HEALER -> "♥";
            case UNDEAD -> "☠";
            case SPLIT -> "❖";
            case ARMORED -> "■";
        };
    }

    public static MutableText element(Element element) {
        if (element == null) {
            return Text.translatable("mctd.element.neutral").formatted(Formatting.WHITE);
        }
        return Text.literal(symbol(element) + " ").append(Text.translatable(element.translationKey()))
                .formatted(color(element));
    }

    public static MutableText kind(CreepKind kind) {
        return Text.translatable(kind.translationKey());
    }

    public static MutableText abilities(Set<Ability> abilities) {
        MutableText text = Text.empty();
        boolean first = true;
        for (Ability a : abilities) {
            if (!first) {
                text.append(", ");
            }
            text.append(Text.translatable(a.translationKey()));
            first = false;
        }
        return text;
    }

    /** "Wave 7: Fire · Fast (Regeneration)" */
    public static MutableText wave(WaveDef def) {
        MutableText text = Text.translatable("mctd.wave.number", def.number()).formatted(Formatting.GOLD)
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(element(def.element()))
                .append(Text.literal(" · ").formatted(Formatting.GRAY))
                .append(kind(def.kind()).formatted(def.kind().bossLike() ? Formatting.DARK_RED : Formatting.WHITE));
        if (!def.spec().abilities().isEmpty()) {
            text.append(Text.literal(" (").formatted(Formatting.GRAY))
                    .append(abilities(def.spec().abilities()).formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.literal(")").formatted(Formatting.GRAY));
        }
        return text;
    }

    public static Formatting towerColor(TowerType type) {
        if (type.elements().isEmpty()) {
            return Formatting.WHITE;
        }
        return switch (type.category()) {
            case DUAL -> Formatting.AQUA;
            case TRIPLE -> Formatting.LIGHT_PURPLE;
            default -> color(type.elements().get(0));
        };
    }

    public static MutableText towerName(TowerType type) {
        return Text.translatable(type.translationKey()).formatted(towerColor(type));
    }

    public static MutableText towerName(TowerType type, int tier) {
        return towerName(type).append(Text.literal(" " + roman(tier)).formatted(Formatting.YELLOW));
    }

    public static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(tier);
        };
    }

    public static String num(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String percent(double share) {
        return num(Math.round(share * 1000) / 10.0);
    }

    public static String seconds(int ticks) {
        return num(Math.round(ticks / 2.0) / 10.0);
    }

    /** Elements of a tower as colored symbols, e.g. "☀ ☾". */
    public static MutableText elementSymbols(List<Element> elements) {
        MutableText text = Text.empty();
        for (Element e : elements) {
            text.append(Text.literal(symbol(e) + " ").formatted(color(e)));
        }
        return text;
    }

    private static MutableText line(String key, Object... args) {
        return Text.translatable(key, args).formatted(Formatting.GRAY);
    }

    /** Stats and effects of a tower tier as tooltip lines. */
    public static List<Text> towerStats(TowerType type, int tier) {
        TowerType.Tier s = type.tier(tier);
        List<Text> lines = new ArrayList<>();
        if (type.elements().isEmpty()) {
            lines.add(line("mctd.stat.element", Text.translatable("mctd.element.neutral").formatted(Formatting.WHITE)));
        } else if (type.adaptive()) {
            lines.add(line("mctd.stat.adaptive", elementSymbols(type.elements())));
        } else {
            lines.add(line("mctd.stat.element", element(type.damageElement())));
        }
        if (type.style() != AttackStyle.SUPPORT) {
            lines.add(line("mctd.stat.damage", Text.literal(num(s.damage)).formatted(Formatting.RED),
                    Text.literal(num(Math.round(s.dps()))).formatted(Formatting.RED)));
            lines.add(line("mctd.stat.speed", Text.literal(num(Math.round(s.attacksPerSecond() * 100) / 100.0))
                    .formatted(Formatting.WHITE)));
        }
        lines.add(line("mctd.stat.range", Text.literal(num(s.range)).formatted(Formatting.WHITE)));
        switch (type.style()) {
            case SPLASH -> lines.add(line("mctd.effect.splash", num(s.splashRadius), percent(s.splashFactor)));
            case MULTI -> lines.add(line("mctd.effect.multi", s.targets));
            case CHAIN -> lines.add(line("mctd.effect.chain", s.targets, percent(1 - s.chainDecay)));
            case PULSE -> lines.add(line("mctd.effect.pulse"));
            case RAMP -> lines.add(line("mctd.effect.ramp", percent(s.ramp), percent(s.rampMax)));
            default -> {
            }
        }
        if (s.slow > 0) {
            lines.add(line("mctd.effect.slow", percent(s.slow), seconds(s.slowTicks)));
        }
        if (s.stunChance > 0) {
            lines.add(line("mctd.effect.stun", percent(s.stunChance), seconds(s.stunTicks)));
        }
        if (s.burn > 0) {
            lines.add(line("mctd.effect.burn", percent(s.burn), seconds(s.burnTicks)));
        }
        if (s.poison > 0) {
            lines.add(line("mctd.effect.poison", percent(s.poison), s.poisonStacks));
        }
        if (s.curse > 0) {
            lines.add(line("mctd.effect.curse", percent(s.curse), seconds(s.curseTicks)));
        }
        if (s.knockback > 0) {
            lines.add(line("mctd.effect.knockback", num(s.knockback)));
        }
        if (s.execute > 0) {
            lines.add(line("mctd.effect.execute", percent(s.execute)));
        }
        if (s.percentHp > 0) {
            lines.add(line("mctd.effect.percent_hp", percent(s.percentHp)));
        }
        if (s.critChance > 0) {
            lines.add(line("mctd.effect.crit", percent(s.critChance), num(s.critMultiplier)));
        }
        if (s.distanceBonus > 0) {
            lines.add(line("mctd.effect.distance", percent(s.distanceBonus)));
        }
        if (s.disease > 0) {
            lines.add(line("mctd.effect.disease", percent(s.disease), seconds(s.diseaseTicks), num(s.diseaseRadius)));
        }
        if (s.auraDamage > 0) {
            lines.add(line("mctd.effect.aura_damage", percent(s.auraDamage)));
        }
        if (s.auraSpeed > 0) {
            lines.add(line("mctd.effect.aura_speed", percent(s.auraSpeed)));
        }
        if (s.bountyBonus > 0) {
            lines.add(line("mctd.effect.bounty", percent(s.bountyBonus)));
        }
        if (s.waveGold > 0) {
            lines.add(line("mctd.effect.wave_gold", s.waveGold));
        }
        if (!type.hitsAir()) {
            lines.add(Text.translatable("mctd.stat.ground_only").formatted(Formatting.DARK_RED));
        }
        return lines;
    }

    /** Requirement line for a tier, green when fulfilled. */
    public static MutableText requirement(TowerType type, int tier, int[] levels) {
        MutableText text = Text.translatable("mctd.stat.requires").formatted(Formatting.GRAY);
        for (Element e : type.elements()) {
            boolean ok = levels[e.ordinal()] >= tier;
            text.append(Text.literal(" " + symbol(e) + tier).formatted(ok ? Formatting.GREEN : Formatting.RED));
        }
        return text;
    }
}
