package io.github.dalpner.mctd.menu;

import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.util.Texts;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Spend element essences on element levels. */
public class ElementMenu extends Menu {
    private final Session session;

    public ElementMenu(ServerPlayerEntity player, Session session) {
        super(player, 3);
        this.session = session;
    }

    @Override
    protected Text title() {
        return Text.translatable("mctd.menu.elements");
    }

    public static Item icon(Element element) {
        return switch (element) {
            case LIGHT -> Items.GLOWSTONE_DUST;
            case DARKNESS -> Items.ECHO_SHARD;
            case WATER -> Items.HEART_OF_THE_SEA;
            case FIRE -> Items.BLAZE_POWDER;
            case NATURE -> Items.OAK_SAPLING;
            case EARTH -> Items.FLINT;
        };
    }

    @Override
    protected void draw() {
        Game game = session.game();
        set(4, icon(Items.AMETHYST_SHARD, Text.translatable("mctd.menu.elements.essences", game.essences())
                .formatted(Formatting.AQUA), List.of(
                Text.translatable("mctd.menu.elements.info.1"),
                Text.translatable("mctd.menu.elements.info.2"),
                Text.translatable("mctd.menu.elements.info.3"))), null);
        Element[] elements = Element.values();
        for (int i = 0; i < elements.length; i++) {
            Element element = elements[i];
            int level = game.level(element);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.translatable("mctd.menu.elements.level", level, Element.MAX_LEVEL).formatted(Formatting.WHITE));
            lore.add(Text.translatable("mctd.menu.elements.strong", Texts.element(element.beats())));
            lore.add(Text.translatable("mctd.menu.elements.weak", Texts.element(element.beatenBy())));
            lore.add(Text.empty());
            if (level >= Element.MAX_LEVEL) {
                lore.add(Text.translatable("mctd.menu.elements.maxed").formatted(Formatting.GOLD));
            } else if (game.essences() <= 0) {
                lore.add(Text.translatable("mctd.menu.elements.no_essence").formatted(Formatting.RED));
            } else {
                lore.add(Text.translatable("mctd.menu.elements.buy", level + 1).formatted(Formatting.GREEN));
                if (game.config().elementGuardians()) {
                    lore.add(Text.translatable("mctd.menu.elements.guardian").formatted(Formatting.YELLOW));
                }
            }
            ItemStack stack = icon(icon(element), Texts.element(element).formatted(Formatting.BOLD), lore);
            stack.setCount(Math.max(1, level));
            if (level > 0) {
                glint(stack);
            }
            set(10 + i + (i >= 3 ? 1 : 0), stack, click -> {
                TdResult result = game.buyElement(element);
                if (result != TdResult.OK) {
                    actionBar(Text.translatable(result.translationKey()).formatted(Formatting.RED));
                }
                redraw();
            });
        }
        set(22, icon(Items.ARROW, Text.translatable("mctd.menu.back").formatted(Formatting.GRAY), List.of()),
                click -> switchTo(new MainMenu(player, session)));
    }
}
