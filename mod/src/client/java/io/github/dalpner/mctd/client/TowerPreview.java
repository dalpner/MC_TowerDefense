package io.github.dalpner.mctd.client;

import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.core.Element;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

/** While holding the builder's wand: shows the range of the tower under the crosshair. */
public final class TowerPreview {
    /** The tower the player looks at. */
    public record Target(BlockPos pos, TowerType type, int tier) {
    }

    private static Target target;
    private static int timer;

    private TowerPreview() {
    }

    public static Target target() {
        return target;
    }

    static void tick(MinecraftClient client) {
        target = null;
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (!player.getMainHandStack().isOf(ModItems.BUILDER_WAND) && !player.getOffHandStack().isOf(ModItems.BUILDER_WAND)) {
            return;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (!(state.getBlock() instanceof TowerBlock tower) || tower.towerType() == null) {
            return;
        }
        int tier = state.get(TowerBlock.TIER);
        target = new Target(pos, tower.towerType(), tier);
        if (++timer % 4 != 0) {
            return;
        }
        TowerType type = tower.towerType();
        double range = type.tier(tier).range;
        Element element = type.damageElement();
        int rgb = element == null ? 0xE0E0E0 : element.color();
        DustParticleEffect dust = new DustParticleEffect(new Vector3f(
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), 0.8f);
        int points = (int) Math.max(24, Math.round(range * Math.PI * 2 / 0.6));
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double y = pos.getY() + 0.05;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            client.world.addParticle(dust, cx + Math.cos(angle) * range, y, cz + Math.sin(angle) * range, 0, 0, 0);
        }
    }
}
