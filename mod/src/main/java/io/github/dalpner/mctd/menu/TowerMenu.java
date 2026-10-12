package io.github.dalpner.mctd.menu;

import io.github.dalpner.mctd.core.TargetMode;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.core.sim.Tower;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.registry.ModBlocks;
import io.github.dalpner.mctd.registry.ModItems;
import io.github.dalpner.mctd.util.Sounds;
import io.github.dalpner.mctd.util.Texts;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Upgrade or sell a tower and choose how it picks its targets. */
public class TowerMenu extends Menu {
    private final Session session;
    private final Tower tower;
    private boolean confirmSell;

    public TowerMenu(ServerPlayerEntity player, Session session, Tower tower) {
        super(player, 3);
        this.session = session;
        this.tower = tower;
    }

    @Override
    protected Text title() {
        return Texts.towerName(tower.type(), tower.tier());
    }

    @Override
    protected void draw() {
        Game game = session.game();
        if (game.towerAt(tower.cellX(), tower.cellZ()) != tower) {
            close();
            return;
        }
        TowerType type = tower.type();
        List<Text> info = new ArrayList<>();
        info.add(Text.translatable(type.descriptionKey()).formatted(Formatting.DARK_AQUA));
        info.addAll(Texts.towerStats(type, tower.tier()));
        info.add(Text.empty());
        info.add(Text.translatable("mctd.menu.tower.kills", tower.kills(), Texts.num(Math.round(tower.damageDealt())))
                .formatted(Formatting.WHITE));
        if (tower.buffDamage() > 0 || tower.buffSpeed() > 0) {
            info.add(Text.translatable("mctd.menu.tower.buffs", Texts.percent(tower.buffDamage()),
                    Texts.percent(tower.buffSpeed())).formatted(Formatting.GREEN));
        }
        if (type.style() == io.github.dalpner.mctd.core.AttackStyle.RAMP) {
            info.add(Text.translatable("mctd.menu.tower.ramp", tower.rampStacks()).formatted(Formatting.GOLD));
        }
        ItemStack self = icon(ModItems.towerItem(ModBlocks.tower(type)), Texts.towerName(type, tower.tier()), info);
        self.setCount(tower.tier());
        set(4, self, null);

        if (tower.maxed()) {
            set(11, glint(icon(Items.NETHER_STAR, Text.translatable("mctd.menu.tower.maxed").formatted(Formatting.GOLD),
                    List.of())), null);
        } else {
            int next = tower.tier() + 1;
            int cost = type.tier(next).cost;
            List<Text> lore = new ArrayList<>();
            if (!type.elements().isEmpty()) {
                lore.add(Texts.requirement(type, next, game.levels()));
            }
            lore.add(Text.translatable("mctd.menu.cost", cost).formatted(game.gold() >= cost ? Formatting.YELLOW : Formatting.RED));
            lore.addAll(Texts.towerStats(type, next));
            TdResult can = game.canUpgrade(tower);
            lore.add(can == TdResult.OK
                    ? Text.translatable("mctd.menu.tower.upgrade.click").formatted(Formatting.GREEN)
                    : Text.translatable(can.translationKey()).formatted(Formatting.RED));
            set(11, icon(Items.ANVIL, Text.translatable("mctd.menu.tower.upgrade", Texts.roman(next))
                    .formatted(Formatting.GREEN), lore), click -> {
                TdResult result = session.upgrade(tower);
                if (result == TdResult.OK) {
                    Sounds.play(player, "block.smithing_table.use", 0.6f, 1.2f);
                    actionBar(Text.translatable("mctd.msg.upgraded", Texts.towerName(type, tower.tier()))
                            .formatted(Formatting.GREEN));
                } else {
                    actionBar(Text.translatable(result.translationKey()).formatted(Formatting.RED));
                }
                redraw();
            });
        }

        int value = game.sellValue(tower);
        List<Text> sellLore = new ArrayList<>();
        sellLore.add(Text.translatable("mctd.menu.tower.sell.value", value).formatted(Formatting.YELLOW));
        if (value == tower.invested()) {
            sellLore.add(Text.translatable("mctd.menu.tower.sell.full").formatted(Formatting.GREEN));
        }
        if (confirmSell) {
            sellLore.add(Text.translatable("mctd.menu.confirm").formatted(Formatting.RED, Formatting.BOLD));
        }
        set(15, icon(Items.GOLD_NUGGET, Text.translatable("mctd.menu.tower.sell").formatted(Formatting.GOLD), sellLore),
                click -> {
                    if (!confirmSell) {
                        confirmSell = true;
                        redraw();
                        return;
                    }
                    int refund = session.sell(tower);
                    if (refund >= 0) {
                        Sounds.play(player, "entity.experience_orb.pickup", 0.6f, 0.8f);
                        actionBar(Text.translatable("mctd.msg.sold", Texts.towerName(type), refund).formatted(Formatting.GOLD));
                    }
                    close();
                });

        if (!type.isSupportOnly()) {
            TargetMode[] modes = TargetMode.values();
            for (int i = 0; i < modes.length; i++) {
                TargetMode mode = modes[i];
                boolean selected = tower.mode() == mode;
                ItemStack stack = icon(modeIcon(mode), Text.translatable(mode.translationKey())
                                .formatted(selected ? Formatting.GREEN : Formatting.WHITE),
                        List.of(Text.translatable(mode.translationKey() + ".desc"),
                                Text.translatable(selected ? "mctd.menu.tower.mode.active" : "mctd.menu.tower.mode.click")
                                        .formatted(selected ? Formatting.GREEN : Formatting.GRAY)));
                set(20 + i, selected ? glint(stack) : stack, click -> {
                    game.setTargetMode(tower, mode);
                    redraw();
                });
            }
        }
        set(26, icon(Items.BARRIER, Text.translatable("mctd.menu.close").formatted(Formatting.RED), List.of()),
                click -> close());
    }

    private static Item modeIcon(TargetMode mode) {
        return switch (mode) {
            case FIRST -> Items.LIME_DYE;
            case LAST -> Items.ORANGE_DYE;
            case STRONGEST -> Items.RED_DYE;
            case WEAKEST -> Items.LIGHT_BLUE_DYE;
            case CLOSEST -> Items.YELLOW_DYE;
        };
    }
}
