package io.github.dalpner.mctd.menu;

import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.WaveDef;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.game.SessionManager;
import io.github.dalpner.mctd.util.Texts;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Game overview: status, elements, next wave, leaving and ending the game. */
public class MainMenu extends Menu {
    private final Session session;
    private boolean confirmStop;

    public MainMenu(ServerPlayerEntity player, Session session) {
        super(player, 3);
        this.session = session;
    }

    @Override
    protected Text title() {
        return Text.translatable("mctd.menu.main");
    }

    @Override
    protected void draw() {
        Game game = session.game();
        set(4, icon(Items.BOOK, Text.translatable("mctd.menu.main.status").formatted(Formatting.GOLD), List.of(
                Text.translatable("mctd.menu.main.status.wave", game.currentWave(), game.maxWaves()),
                Text.translatable("mctd.menu.main.status.gold", game.gold()).formatted(Formatting.YELLOW),
                Text.translatable("mctd.menu.main.status.lives", game.lives()).formatted(Formatting.RED),
                Text.translatable("mctd.menu.main.status.essences", game.essences()).formatted(Formatting.AQUA),
                Text.translatable("mctd.menu.main.status.kills", game.kills(), game.leaks()),
                Text.translatable("mctd.menu.main.status.settings",
                        Text.translatable(game.config().difficulty().translationKey()),
                        Text.translatable(game.config().layout().translationKey())))), null);

        set(10, glintIf(icon(Items.NETHER_STAR, Text.translatable("mctd.menu.main.elements").formatted(Formatting.AQUA),
                List.of(Text.translatable("mctd.menu.main.elements.info", game.essences()).formatted(Formatting.AQUA))),
                game.essences() > 0), click -> switchTo(new ElementMenu(player, session)));

        if (!game.isOver()) {
            List<Text> lore = new ArrayList<>();
            if (game.nextWave() <= game.maxWaves() || game.endless()) {
                lore.add(Texts.wave(game.waves().wave(game.nextWave())));
            }
            if (game.phase() == Game.Phase.BUILD) {
                lore.add(Text.translatable("mctd.menu.main.next.countdown", Texts.seconds(game.countdown())));
                lore.add(Text.translatable("mctd.menu.main.next.bonus",
                        Game.earlyBonus(game.countdown(), game.nextWave())).formatted(Formatting.YELLOW));
            } else {
                lore.add(Text.translatable("mctd.menu.main.next.running").formatted(Formatting.RED));
            }
            set(12, icon(Items.CLOCK, Text.translatable("mctd.menu.main.next").formatted(Formatting.GREEN), lore), click -> {
                TdResult result = game.callNextWave();
                if (result == TdResult.OK) {
                    if (game.lastEarlyBonus() > 0) {
                        actionBar(Text.translatable("mctd.msg.early_bonus", game.lastEarlyBonus()).formatted(Formatting.YELLOW));
                    }
                    close();
                } else {
                    actionBar(Text.translatable(result.translationKey()).formatted(Formatting.RED));
                }
            });
        }

        List<Text> preview = new ArrayList<>();
        for (int n = game.nextWave(); n < game.nextWave() + 6; n++) {
            if (n > game.maxWaves() && !game.endless()) {
                break;
            }
            WaveDef def = game.waves().wave(n);
            preview.add(Texts.wave(def));
        }
        set(14, icon(Items.SPYGLASS, Text.translatable("mctd.menu.main.preview").formatted(Formatting.LIGHT_PURPLE), preview), null);

        set(16, icon(Items.KNOWLEDGE_BOOK, Text.translatable("mctd.menu.main.help").formatted(Formatting.YELLOW),
                List.of(Text.translatable("mctd.menu.main.help.info"))), click -> {
            close();
            player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource(), "td help");
        });

        if (player.getWorld() == session.world()) {
            set(20, icon(Items.OAK_DOOR, Text.translatable("mctd.menu.main.leave").formatted(Formatting.YELLOW),
                    List.of(Text.translatable("mctd.menu.main.leave.info"))), click -> {
                close();
                SessionManager.leave(player);
            });
        } else {
            set(20, icon(Items.ENDER_PEARL, Text.translatable("mctd.menu.main.return").formatted(Formatting.GREEN),
                    List.of(Text.translatable("mctd.menu.main.return.info"))), click -> {
                close();
                SessionManager.returnToArena(player);
            });
        }

        if (game.isOver()) {
            set(22, icon(Items.RECOVERY_COMPASS, Text.translatable("mctd.menu.main.restart").formatted(Formatting.GREEN),
                    List.of(Text.translatable("mctd.menu.main.restart.info"))), click -> {
                close();
                session.restart(session.world().getRandom().nextLong());
            });
            if (game.phase() == Game.Phase.VICTORY && !game.endless()) {
                set(13, glint(icon(Items.DRAGON_HEAD, Text.translatable("mctd.menu.main.endless").formatted(Formatting.LIGHT_PURPLE),
                        List.of(Text.translatable("mctd.menu.main.endless.info")))), click -> {
                    if (game.continueEndless()) {
                        session.broadcast(Text.translatable("mctd.msg.endless").formatted(Formatting.LIGHT_PURPLE));
                    }
                    close();
                });
            }
        }

        boolean mayStop = player.getUuid().equals(session.owner()) || player.hasPermissionLevel(2)
                || player.getServer().getPlayerManager().getPlayer(session.owner()) == null;
        List<Text> stopLore = new ArrayList<>();
        stopLore.add(Text.translatable(mayStop ? "mctd.menu.main.stop.info" : "mctd.menu.main.quit.info"));
        if (confirmStop) {
            stopLore.add(Text.translatable("mctd.menu.confirm").formatted(Formatting.RED, Formatting.BOLD));
        }
        set(24, icon(Items.BARRIER, Text.translatable(mayStop ? "mctd.menu.main.stop" : "mctd.menu.main.quit")
                .formatted(Formatting.RED), stopLore), click -> {
            if (!confirmStop) {
                confirmStop = true;
                redraw();
                return;
            }
            close();
            if (mayStop) {
                SessionManager.stop(session);
            } else {
                SessionManager.quit(player);
            }
        });
    }

    private static net.minecraft.item.ItemStack glintIf(net.minecraft.item.ItemStack stack, boolean glint) {
        return glint ? glint(stack) : stack;
    }
}
