package io.github.dalpner.mctd.registry;

import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.item.BuilderWandItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.Map;

public final class ModItems {
    public static Item BUILDER_WAND;
    private static final Map<TowerBlock, Item> TOWER_ITEMS = new HashMap<>();

    private ModItems() {
    }

    public static void register() {
        BUILDER_WAND = Registry.register(Registries.ITEM, MctdMod.id("builder_wand"),
                new BuilderWandItem(new Item.Settings()
                        .maxCount(1)
                        .rarity(Rarity.EPIC)
                        .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));
        for (TowerBlock block : ModBlocks.towers()) {
            Item item = Registry.register(Registries.ITEM, Registries.BLOCK.getId(block),
                    new BlockItem(block, new Item.Settings()));
            TOWER_ITEMS.put(block, item);
        }
        Registry.register(Registries.ITEM_GROUP, MctdMod.id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(BUILDER_WAND))
                .displayName(Text.translatable("itemGroup.mctd.main"))
                .entries((context, entries) -> {
                    entries.add(BUILDER_WAND);
                    for (TowerBlock block : ModBlocks.towers()) {
                        entries.add(TOWER_ITEMS.get(block));
                    }
                })
                .build());
    }

    public static Item towerItem(TowerBlock block) {
        return TOWER_ITEMS.get(block);
    }
}
