package io.github.dalpner.mctd.core;

import java.util.Locale;

/** Prints the estimated damage per gold of every tower tier (development helper). */
public final class TowerReport {
    public static void main(String[] args) {
        for (TowerType t : Towers.all()) {
            StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "%-11s %-8s", t.id(), t.category().id()));
            for (int tier = 1; tier <= 3; tier++) {
                TowerType.Tier s = t.tier(tier);
                double eff = BalanceSim.Bot.effectiveDps(t, tier);
                int total = t.totalCost(tier);
                sb.append(String.format(Locale.ROOT, " | T%d cost %5d dmg %6.0f raw %6.1f eff %7.1f eff/g %.3f",
                        tier, total, s.damage, s.dps(), eff, eff / total));
            }
            System.out.println(sb);
        }
    }
}
