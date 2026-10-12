package io.github.dalpner.mctd.game;

import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.core.TdResult;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.sim.Tower;
import io.github.dalpner.mctd.menu.BuildMenu;
import io.github.dalpner.mctd.menu.LobbyMenu;
import io.github.dalpner.mctd.menu.MainMenu;
import io.github.dalpner.mctd.menu.TowerMenu;
import io.github.dalpner.mctd.registry.ModItems;
import io.github.dalpner.mctd.util.Sounds;
import io.github.dalpner.mctd.util.Texts;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Handles the builder's wand. */
public final class Interactions {
    private Interactions() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!player.getStackInHand(hand).isOf(ModItems.BUILDER_WAND) || player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                useOnBlock(serverPlayer, hit.getBlockPos());
            }
            return ActionResult.SUCCESS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                entity.getCommandTags().contains(Session.CREEP_TAG) ? ActionResult.FAIL : ActionResult.PASS);
    }

    /** Right-click into the air: lobby or main menu. */
    public static void useInAir(ServerPlayerEntity player) {
        Session session = SessionManager.byMember(player.getUuid());
        if (session == null) {
            new LobbyMenu(player).open();
        } else {
            new MainMenu(player, session).open();
        }
    }

    private static void useOnBlock(ServerPlayerEntity player, BlockPos pos) {
        Session session = SessionManager.byMember(player.getUuid());
        if (session == null || player.getWorld() != session.world() || !session.contains(pos)) {
            useInAir(player);
            return;
        }
        BlockState state = player.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof TowerBlock) {
            Tower tower = session.towerAt(pos);
            if (tower != null) {
                new TowerMenu(player, session, tower).open();
            }
            return;
        }
        int floor = session.origin().getY();
        BlockPos target;
        if (pos.getY() == floor) {
            target = pos.up();
        } else if (pos.getY() == floor + 1 && Session.isFree(state)) {
            target = pos;
        } else {
            player.sendMessage(Text.translatable("mctd.result.not_buildable").formatted(Formatting.RED), true);
            return;
        }
        int x = target.getX() - session.origin().getX();
        int z = target.getZ() - session.origin().getZ();
        if (!session.layout().isBuildable(x, z)) {
            player.sendMessage(Text.translatable("mctd.result.not_buildable").formatted(Formatting.RED), true);
            return;
        }
        TowerType last = session.lastBuilt(player.getUuid());
        if (player.isSneaking() && last != null) {
            TdResult result = session.build(player, last, target);
            if (result == TdResult.OK) {
                Sounds.play(player, "block.anvil.place", 0.5f, 1.4f);
                player.sendMessage(Text.translatable("mctd.msg.built", Texts.towerName(last), last.tier(1).cost)
                        .formatted(Formatting.GREEN), true);
            } else {
                player.sendMessage(Text.translatable(result.translationKey()).formatted(Formatting.RED), true);
            }
            return;
        }
        new BuildMenu(player, session, target).open();
    }
}
