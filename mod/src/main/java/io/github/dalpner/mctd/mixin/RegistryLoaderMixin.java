package io.github.dalpner.mctd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dalpner.mctd.MctdMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.entry.RegistryEntryInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Registry entries from this mod's data (the arena's dimension and dimension type) are stable as well. */
@Mixin(RegistryLoader.class)
public abstract class RegistryLoaderMixin {
    @ModifyVariable(method = "parseAndAdd", at = @At("HEAD"), argsOnly = true)
    private static RegistryEntryInfo mctd$stableEntries(RegistryEntryInfo info, @Local(argsOnly = true) RegistryKey<?> key) {
        return key.getValue().getNamespace().equals(MctdMod.MOD_ID) ? RegistryEntryInfo.DEFAULT : info;
    }
}
