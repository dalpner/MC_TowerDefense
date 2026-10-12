package io.github.dalpner.mctd.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

/**
 * A chest screen whose items cannot be taken. Clicks are forwarded to the {@link Menu}; everything else is
 * ignored and the client is re-synchronised, so vanilla clients see a normal chest GUI.
 */
public class MenuScreenHandler extends GenericContainerScreenHandler {
    private final Menu menu;

    public MenuScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                             SimpleInventory inventory, int rows, Menu menu) {
        super(type, syncId, playerInventory, inventory, rows);
        this.menu = menu;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < getRows() * 9
                && (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE)) {
            menu.click(slotIndex, button, actionType == SlotActionType.QUICK_MOVE);
        }
        if (player.currentScreenHandler == this) {
            syncState();
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
