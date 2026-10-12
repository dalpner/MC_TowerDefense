package io.github.dalpner.mctd.menu;

import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.game.SessionManager;
import io.github.dalpner.mctd.util.Texts;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shown to players without a game: start a new one or join a friend. */
public class LobbyMenu extends Menu {

    public LobbyMenu(ServerPlayerEntity player) {
        super(player, 6);
    }

    @Override
    protected Text title() {
        return Text.translatable("mctd.menu.lobby");
    }

    @Override
    protected void draw() {
        set(4, icon(Items.WRITABLE_BOOK, Text.translatable("mctd.menu.lobby.info").formatted(Formatting.GOLD), List.of(
                Text.translatable("mctd.menu.lobby.info.1"),
                Text.translatable("mctd.menu.lobby.info.2"),
                Text.translatable("mctd.menu.lobby.info.3"))), null);
        ArenaLayout[] layouts = ArenaLayout.values();
        for (int row = 0; row < layouts.length; row++) {
            ArenaLayout layout = layouts[row];
            int base = (row + 1) * 9;
            set(base + 1, icon(layoutIcon(layout), Text.translatable(layout.translationKey()).formatted(Formatting.YELLOW),
                    List.of(Text.translatable(layout.translationKey() + ".desc"),
                            Text.translatable("mctd.menu.lobby.path_length", Texts.num(Math.round(layout.path().length()))))), null);
            Difficulty[] difficulties = Difficulty.values();
            for (int col = 0; col < difficulties.length; col++) {
                Difficulty difficulty = difficulties[col];
                set(base + 3 + col * 2, icon(difficultyIcon(difficulty),
                        Text.translatable(difficulty.translationKey()).formatted(difficultyColor(difficulty)),
                        List.of(Text.translatable("mctd.menu.lobby.difficulty", difficulty.lives(), difficulty.startGold(),
                                        Texts.percent(difficulty.hpMultiplier())),
                                Text.translatable(layout.translationKey()).formatted(Formatting.YELLOW),
                                Text.translatable("mctd.menu.lobby.click_start").formatted(Formatting.GREEN))),
                        click -> {
                            close();
                            SessionManager.start(player, difficulty, layout);
                        });
            }
        }
        List<Session> open = new ArrayList<>(SessionManager.sessions());
        int slot = 45;
        for (Session session : open) {
            if (slot > 53) {
                break;
            }
            String ownerName = ownerName(session.owner());
            set(slot++, icon(Items.NAME_TAG, Text.translatable("mctd.menu.lobby.join", ownerName).formatted(Formatting.AQUA),
                    List.of(Text.translatable("mctd.menu.lobby.join.info", session.game().currentWave(),
                                    Text.translatable(session.game().config().difficulty().translationKey()),
                                    session.members().size()),
                            Text.translatable("mctd.menu.lobby.click_join").formatted(Formatting.GREEN))),
                    click -> {
                        close();
                        SessionManager.join(player, session);
                    });
        }
    }

    private String ownerName(UUID uuid) {
        ServerPlayerEntity owner = player.getServer().getPlayerManager().getPlayer(uuid);
        return owner != null ? owner.getGameProfile().getName() : uuid.toString().substring(0, 8);
    }

    private static Item layoutIcon(ArenaLayout layout) {
        return switch (layout) {
            case SERPENTINE -> Items.MAP;
            case SPIRAL -> Items.NAUTILUS_SHELL;
            case ZIGZAG -> Items.BLAZE_ROD;
        };
    }

    private static Item difficultyIcon(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Items.LIME_CONCRETE;
            case NORMAL -> Items.YELLOW_CONCRETE;
            case HARD -> Items.RED_CONCRETE;
        };
    }

    private static Formatting difficultyColor(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Formatting.GREEN;
            case NORMAL -> Formatting.YELLOW;
            case HARD -> Formatting.RED;
        };
    }
}
