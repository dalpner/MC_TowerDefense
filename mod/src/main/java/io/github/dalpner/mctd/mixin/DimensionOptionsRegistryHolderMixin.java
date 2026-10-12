package io.github.dalpner.mctd.mixin;

import com.mojang.serialization.Lifecycle;
import io.github.dalpner.mctd.MctdMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft treats every dimension that is not the overworld, nether or end as an experimental world setting
 * and warns about it when a world is created or loaded. The arena is a plain void dimension, so it counts as
 * stable and players are not asked to confirm or back up their world.
 */
@Mixin(DimensionOptionsRegistryHolder.class)
public abstract class DimensionOptionsRegistryHolderMixin {
    @Inject(method = "getLifecycle(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/dimension/DimensionOptions;)Lcom/mojang/serialization/Lifecycle;",
            at = @At("HEAD"), cancellable = true)
    private static void mctd$arenaIsStable(RegistryKey<DimensionOptions> key, DimensionOptions options,
                                           CallbackInfoReturnable<Lifecycle> cir) {
        if (key.getValue().getNamespace().equals(MctdMod.MOD_ID)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }
}
