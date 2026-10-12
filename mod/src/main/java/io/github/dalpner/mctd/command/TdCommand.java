package io.github.dalpner.mctd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dalpner.mctd.core.ArenaLayout;
import io.github.dalpner.mctd.core.Difficulty;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.sim.Game;
import io.github.dalpner.mctd.game.Session;
import io.github.dalpner.mctd.game.SessionManager;
import io.github.dalpner.mctd.menu.ElementMenu;
import io.github.dalpner.mctd.menu.LobbyMenu;
import io.github.dalpner.mctd.menu.MainMenu;
import io.github.dalpner.mctd.util.Texts;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

/**
 * {@code /td} – everything can also be done with the builder's wand, the command is a fallback and offers
 * admin helpers.
 */
public final class TdCommand {
    private TdCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("td")
                .executes(TdCommand::help)
                .then(CommandManager.literal("help").executes(TdCommand::help))
                .then(CommandManager.literal("lobby").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    Session session = SessionManager.byMember(player.getUuid());
                    if (session != null) {
                        new MainMenu(player, session).open();
                    } else {
                        new LobbyMenu(player).open();
                    }
                    return 1;
                }))
                .then(CommandManager.literal("start")
                        .executes(ctx -> start(ctx, Difficulty.NORMAL.id(), ArenaLayout.SERPENTINE.id()))
                        .then(CommandManager.argument("difficulty", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                        Arrays.stream(Difficulty.values()).map(Difficulty::id), builder))
                                .executes(ctx -> start(ctx, StringArgumentType.getString(ctx, "difficulty"),
                                        ArenaLayout.SERPENTINE.id()))
                                .then(CommandManager.argument("layout", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                Arrays.stream(ArenaLayout.values()).map(ArenaLayout::id), builder))
                                        .executes(ctx -> start(ctx, StringArgumentType.getString(ctx, "difficulty"),
                                                StringArgumentType.getString(ctx, "layout"))))))
                .then(CommandManager.literal("join")
                        .then(CommandManager.argument("player", EntityArgumentType.player()).executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                            Session session = SessionManager.byMember(target.getUuid());
                            if (session == null) {
                                ctx.getSource().sendError(Text.translatable("mctd.msg.target_not_in_game"));
                                return 0;
                            }
                            return SessionManager.join(player, session) ? 1 : 0;
                        })))
                .then(CommandManager.literal("menu").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    Session session = requireSession(ctx);
                    new MainMenu(player, session).open();
                    return 1;
                }))
                .then(CommandManager.literal("elements").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    new ElementMenu(player, requireSession(ctx)).open();
                    return 1;
                }))
                .then(CommandManager.literal("next").executes(ctx -> {
                    Game game = requireSession(ctx).game();
                    TdResult result = game.callNextWave();
                    if (result != TdResult.OK) {
                        ctx.getSource().sendError(Text.translatable(result.translationKey()));
                        return 0;
                    }
                    if (game.lastEarlyBonus() > 0) {
                        ctx.getSource().sendFeedback(() -> Text.translatable("mctd.msg.early_bonus", game.lastEarlyBonus())
                                .formatted(Formatting.YELLOW), false);
                    }
                    return 1;
                }))
                .then(CommandManager.literal("info").executes(TdCommand::info))
                .then(CommandManager.literal("leave").executes(ctx -> {
                    SessionManager.leave(ctx.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("return").executes(ctx -> {
                    SessionManager.returnToArena(ctx.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("quit").executes(ctx -> {
                    SessionManager.quit(ctx.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("stop").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    Session session = requireSession(ctx);
                    boolean ownerOnline = player.getServer().getPlayerManager().getPlayer(session.owner()) != null;
                    if (!player.getUuid().equals(session.owner()) && ownerOnline && !ctx.getSource().hasPermissionLevel(2)) {
                        ctx.getSource().sendError(Text.translatable("mctd.msg.only_owner"));
                        return 0;
                    }
                    SessionManager.stop(session);
                    return 1;
                }))
                .then(CommandManager.literal("admin").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("gold")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer()).executes(ctx -> {
                                    Game game = requireSession(ctx).game();
                                    game.addGold(IntegerArgumentType.getInteger(ctx, "amount"));
                                    return feedback(ctx, "gold", game.gold());
                                })))
                        .then(CommandManager.literal("essence")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer()).executes(ctx -> {
                                    Game game = requireSession(ctx).game();
                                    game.addEssences(IntegerArgumentType.getInteger(ctx, "amount"));
                                    return feedback(ctx, "essences", game.essences());
                                })))
                        .then(CommandManager.literal("lives")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
                                    Game game = requireSession(ctx).game();
                                    game.setLives(IntegerArgumentType.getInteger(ctx, "amount"));
                                    return feedback(ctx, "lives", game.lives());
                                })))
                        .then(CommandManager.literal("list").executes(ctx -> {
                            for (Session s : SessionManager.sessions()) {
                                ctx.getSource().sendFeedback(() -> Text.literal("#" + s.slot() + " owner=" + s.owner()
                                        + " wave=" + s.game().currentWave() + " members=" + s.members().size()
                                        + (s.paused() ? " (paused)" : "")), false);
                            }
                            return SessionManager.sessions().size();
                        }))));
    }

    private static Session requireSession(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        Session session = SessionManager.byMember(player.getUuid());
        if (session == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Text.translatable("mctd.msg.not_in_game")).create();
        }
        return session;
    }

    private static int feedback(CommandContext<ServerCommandSource> ctx, String what, int value) {
        ctx.getSource().sendFeedback(() -> Text.literal(what + " = " + value).formatted(Formatting.GRAY), true);
        return 1;
    }

    private static int start(CommandContext<ServerCommandSource> ctx, String difficultyId, String layoutId)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        Difficulty difficulty = Difficulty.byId(difficultyId);
        ArenaLayout layout = ArenaLayout.byId(layoutId);
        if (difficulty == null || layout == null) {
            ctx.getSource().sendError(Text.translatable("mctd.msg.bad_arguments"));
            return 0;
        }
        return SessionManager.start(player, difficulty, layout) != null ? 1 : 0;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Game game = requireSession(ctx).game();
        MutableText levels = Text.empty();
        for (Element e : Element.values()) {
            levels.append(Text.literal(Texts.symbol(e) + game.level(e) + " ").formatted(Texts.color(e)));
        }
        ctx.getSource().sendFeedback(() -> Text.translatable("mctd.msg.info", game.currentWave(), game.maxWaves(),
                game.gold(), game.lives(), game.essences(), levels).formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        source.sendFeedback(() -> Text.translatable("mctd.help.title").formatted(Formatting.GOLD, Formatting.BOLD), false);
        for (int i = 1; i <= 9; i++) {
            String key = "mctd.help." + i;
            source.sendFeedback(() -> Text.translatable(key).formatted(Formatting.GRAY), false);
        }
        MutableText wheel = Text.translatable("mctd.help.wheel").formatted(Formatting.GRAY);
        for (Element e : Element.values()) {
            wheel.append(Text.literal(" " + Texts.symbol(e)).formatted(Texts.color(e)))
                    .append(Text.translatable(e.translationKey()).formatted(Texts.color(e)))
                    .append(Text.literal(" →").formatted(Formatting.DARK_GRAY));
        }
        wheel.append(Text.literal(" " + Texts.symbol(Element.LIGHT)).formatted(Texts.color(Element.LIGHT)));
        source.sendFeedback(() -> wheel, false);
        source.sendFeedback(() -> Text.translatable("mctd.help.commands").formatted(Formatting.DARK_AQUA), false);
        return 1;
    }
}
