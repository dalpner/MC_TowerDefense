package io.github.dalpner.mctd.item;

import io.github.dalpner.mctd.game.Interactions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * The builder's wand. Right-click the ground to build, a tower to upgrade or sell it, and the air to open
 * the main menu. Clicks on blocks are handled in {@link Interactions} so they also work in adventure mode.
 */
public class BuilderWandItem extends Item {

    public BuilderWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            Interactions.useInAir(player);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.mctd.builder_wand.tooltip.ground").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.mctd.builder_wand.tooltip.tower").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.mctd.builder_wand.tooltip.sneak").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.mctd.builder_wand.tooltip.air").formatted(Formatting.GRAY));
    }
}
