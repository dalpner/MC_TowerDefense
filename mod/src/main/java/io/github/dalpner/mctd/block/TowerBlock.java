package io.github.dalpner.mctd.block;

import io.github.dalpner.mctd.core.TowerType;
import io.github.dalpner.mctd.core.Towers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * A tower. The block itself is only the visible part: stats and targeting live in the game core.
 * The {@link #TIER} property selects the model of the upgrade level.
 */
public class TowerBlock extends Block {
    public static final IntProperty TIER = IntProperty.of("tier", 1, 3);

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 3, 16),
            Block.createCuboidShape(2, 3, 2, 14, 16, 14));

    private final String towerId;

    public TowerBlock(Settings settings, String towerId) {
        super(settings);
        this.towerId = towerId;
        setDefaultState(getStateManager().getDefaultState().with(TIER, 1));
    }

    public String towerId() {
        return towerId;
    }

    public TowerType towerType() {
        return Towers.byId(towerId);
    }

    public BlockState withTier(int tier) {
        return getDefaultState().with(TIER, Math.max(1, Math.min(3, tier)));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
