package io.github.dalpner.mctd.menu;

import io.github.dalpner.mctd.util.Sounds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Base class of the server-side chest menus. */
public abstract class Menu {
    /** A click on a menu slot. */
    public record Click(int button, boolean shift) {
        public boolean right() {
            return button == 1;
        }
    }

    protected final ServerPlayerEntity player;
    private final int rows;
    private final SimpleInventory inventory;
    private final Map<Integer, Consumer<Click>> actions = new HashMap<>();

    protected Menu(ServerPlayerEntity player, int rows) {
        this.player = player;
        this.rows = rows;
        this.inventory = new SimpleInventory(rows * 9);
    }

    protected abstract Text title();

    /** Fills the menu; called on open and after every {@link #redraw()}. */
    protected abstract void draw();

    public void open() {
        redraw();
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new MenuScreenHandler(type(rows), syncId, playerInventory, inventory, rows, this),
                title()));
    }

    public void redraw() {
        actions.clear();
        ItemStack filler = icon(Items.GRAY_STAINED_GLASS_PANE, Text.literal(" "), List.of());
        filler.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        for (int i = 0; i < rows * 9; i++) {
            inventory.setStack(i, filler.copy());
        }
        draw();
    }

    void click(int slot, int button, boolean shift) {
        Consumer<Click> action = actions.get(slot);
        if (action != null) {
            Sounds.play(player, "ui.button.click", 0.35f, 1.2f);
            action.accept(new Click(button, shift));
        }
    }

    protected void set(int slot, ItemStack icon, Consumer<Click> action) {
        if (slot < 0 || slot >= rows * 9) {
            return;
        }
        inventory.setStack(slot, icon);
        if (action != null) {
            actions.put(slot, action);
        } else {
            actions.remove(slot);
        }
    }

    protected void close() {
        player.closeHandledScreen();
    }

    /** Opens another menu on the next server task (never from inside a click). */
    protected void switchTo(Menu next) {
        player.getServer().execute(next::open);
    }

    protected void actionBar(Text text) {
        player.sendMessage(text, true);
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> type(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    // ------------------------------------------------------------------ icons

    /** An item with a name and tooltip lines; italics are removed and uncoloured lines become gray. */
    public static ItemStack icon(ItemConvertible item, Text name, List<Text> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, name.copy().styled(s -> s.withItalic(false)));
        List<Text> lines = new ArrayList<>(lore.size());
        for (Text line : lore) {
            lines.add(line.copy().styled(s -> s.withItalic(false).withColor(s.getColor() == null
                    ? net.minecraft.text.TextColor.fromFormatting(Formatting.GRAY) : s.getColor())));
        }
        stack.set(DataComponentTypes.LORE, new LoreComponent(lines));
        stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return stack;
    }

    public static ItemStack glint(ItemStack stack) {
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
}
