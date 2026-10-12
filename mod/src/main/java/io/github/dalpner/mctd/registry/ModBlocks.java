package io.github.dalpner.mctd.registry;

import io.github.dalpner.mctd.MctdMod;
import io.github.dalpner.mctd.block.TowerBlock;
import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.Towers;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One block per tower type, registered as {@code mctd:<tower>_tower}. */
public final class ModBlocks {
    private static final Map<String, TowerBlock> TOWERS = new LinkedHashMap<>();

    private ModBlocks() {
    }

    public static void register() {
        for (TowerType type : Towers.all()) {
            TowerBlock block = new TowerBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .strength(-1.0f, 3600000.0f)
                    .dropsNothing()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.STONE)
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .luminance(state -> 4 + 3 * state.get(TowerBlock.TIER)), type.id());
            Registry.register(Registries.BLOCK, MctdMod.id(blockPath(type)), block);
            TOWERS.put(type.id(), block);
        }
    }

    public static String blockPath(TowerType type) {
        return type.id() + "_tower";
    }

    public static TowerBlock tower(TowerType type) {
        return TOWERS.get(type.id());
    }

    public static Collection<TowerBlock> towers() {
        return Collections.unmodifiableCollection(TOWERS.values());
    }
}
