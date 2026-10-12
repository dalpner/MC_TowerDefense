package io.github.dalpner.mctd.menu;

import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.Towers;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.registry.ModBlocks;
import io.github.dalpner.mctd.registry.ModItems;
import io.github.dalpner.mctd.util.Sounds;
import io.github.dalpner.mctd.util.Texts;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Choose a tower to build on the clicked spot. Page 0: basic, element and dual towers; page 1: triple towers. */
public class BuildMenu extends Menu {
    private final Session session;
    private final BlockPos pos;
    private int page;

    public BuildMenu(ServerPlayerEntity player, Session session, BlockPos pos) {
        super(player, 6);
        this.session = session;
        this.pos = pos;
    }

    @Override
    protected Text title() {
        return Text.translatable("mctd.menu.build");
    }

    @Override
    protected void draw() {
        Game game = session.game();
        set(4, icon(Items.GOLD_INGOT, Text.translatable("mctd.menu.build.gold", game.gold()).formatted(Formatting.GOLD),
                List.of(Text.translatable("mctd.menu.build.levels", levels(game)),
                        Text.translatable("mctd.menu.build.hint"))), null);
        set(8, icon(Items.BARRIER, Text.translatable("mctd.menu.close").formatted(Formatting.RED), List.of()),
                click -> close());
        if (page == 0) {
            List<TowerType> basic = Towers.byCategory(TowerType.Category.BASIC);
            for (int i = 0; i < basic.size(); i++) {
                towerButton(i, basic.get(i));
            }
            List<TowerType> single = Towers.byCategory(TowerType.Category.ELEMENT);
            for (int i = 0; i < single.size(); i++) {
                towerButton(9 + i, single.get(i));
            }
            List<TowerType> dual = Towers.byCategory(TowerType.Category.DUAL);
            for (int i = 0; i < dual.size(); i++) {
                towerButton(18 + i, dual.get(i));
            }
            set(49, icon(Items.NETHER_STAR, Text.translatable("mctd.menu.build.triples").formatted(Formatting.LIGHT_PURPLE),
                    List.of(Text.translatable("mctd.menu.build.triples.info"))), click -> {
                page = 1;
                redraw();
            });
        } else {
            List<TowerType> triple = Towers.byCategory(TowerType.Category.TRIPLE);
            for (int i = 0; i < triple.size(); i++) {
                towerButton(9 + i, triple.get(i));
            }
            set(45, icon(Items.ARROW, Text.translatable("mctd.menu.back").formatted(Formatting.GRAY), List.of()), click -> {
                page = 0;
                redraw();
            });
        }
    }

    private static Text levels(Game game) {
        net.minecraft.text.MutableText text = Text.empty();
        for (Element e : Element.values()) {
            text.append(Text.literal(Texts.symbol(e) + game.level(e) + " ").formatted(Texts.color(e)));
        }
        return text;
    }

    private void towerButton(int slot, TowerType type) {
        Game game = session.game();
        int[] levels = game.levels();
        boolean unlocked = type.unlocked(levels, 1);
        int cost = type.tier(1).cost;
        boolean affordable = game.gold() >= cost;
        List<Text> lore = new ArrayList<>();
        if (!type.elements().isEmpty()) {
            lore.add(Texts.requirement(type, 1, levels));
        }
        lore.add(Text.translatable("mctd.menu.cost", cost).formatted(affordable ? Formatting.YELLOW : Formatting.RED));
        lore.add(Text.translatable(type.descriptionKey()).formatted(Formatting.DARK_AQUA));
        lore.addAll(Texts.towerStats(type, 1));
        if (!unlocked) {
            lore.add(Text.translatable("mctd.menu.locked").formatted(Formatting.RED));
        } else if (!affordable) {
            lore.add(Text.translatable("mctd.result.not_enough_gold").formatted(Formatting.RED));
        } else {
            lore.add(Text.translatable("mctd.menu.build.click").formatted(Formatting.GREEN));
        }
        ItemStack stack = unlocked
                ? icon(ModItems.towerItem(ModBlocks.tower(type)), Texts.towerName(type), lore)
                : icon(Items.GRAY_DYE, Texts.towerName(type).formatted(Formatting.STRIKETHROUGH), lore);
        set(slot, stack, click -> {
            TdResult result = session.build(player, type, pos);
            if (result == TdResult.OK) {
                Sounds.play(player, "block.anvil.place", 0.5f, 1.4f);
                actionBar(Text.translatable("mctd.msg.built", Texts.towerName(type), cost).formatted(Formatting.GREEN));
                close();
            } else {
                actionBar(Text.translatable(result.translationKey()).formatted(Formatting.RED));
                redraw();
            }
        });
    }
}
