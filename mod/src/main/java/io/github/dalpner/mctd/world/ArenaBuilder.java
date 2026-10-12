package io.github.dalpner.mctd.world;

import io.github.dalpner.mctd.core.ArenaLayout;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.SplittableRandom;

/** Places and removes the blocks of an arena. The floor of the arena is at {@code origin.getY()}. */
public final class ArenaBuilder {
    private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
    private static final int CLEAR_BELOW = 3;
    private static final int CLEAR_ABOVE = 10;
    private static final int GATE_HEIGHT = 7;

    private ArenaBuilder() {
    }

    /** Loads (and optionally force-loads) all chunks covered by an arena. */
    public static void loadChunks(ServerWorld world, BlockPos origin, ArenaLayout layout, boolean forced) {
        int minCx = (origin.getX() - 2) >> 4;
        int maxCx = (origin.getX() + layout.size() + 2) >> 4;
        int minCz = (origin.getZ() - 2) >> 4;
        int maxCz = (origin.getZ() + layout.size() + 2) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                world.getChunk(cx, cz);
                world.setChunkForced(cx, cz, forced);
            }
        }
    }

    public static void build(ServerWorld world, BlockPos origin, ArenaLayout layout, long seed) {
        loadChunks(world, origin, layout, true);
        clear(world, origin, layout);
        SplittableRandom random = new SplittableRandom(seed);
        int size = layout.size();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                byte cell = layout.cell(x, z);
                set(world, pos.set(origin.getX() + x, origin.getY() - 2, origin.getZ() + z), Blocks.STONE.getDefaultState());
                BlockState below = cell == ArenaLayout.BORDER ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.DIRT.getDefaultState();
                set(world, pos.set(origin.getX() + x, origin.getY() - 1, origin.getZ() + z), below);
                BlockState top;
                BlockState above = null;
                switch (cell) {
                    case ArenaLayout.PATH -> top = Blocks.DIRT_PATH.getDefaultState();
                    case ArenaLayout.BORDER -> {
                        top = borderBrick(random);
                        above = borderBrick(random);
                    }
                    default -> {
                        top = Blocks.GRASS_BLOCK.getDefaultState();
                        if (cell == ArenaLayout.BUILD) {
                            above = plant(random);
                        }
                    }
                }
                set(world, pos.set(origin.getX() + x, origin.getY(), origin.getZ() + z), top);
                if (above != null) {
                    set(world, pos.set(origin.getX() + x, origin.getY() + 1, origin.getZ() + z), above);
                }
                if (cell == ArenaLayout.BORDER && (x + z) % 8 == 0) {
                    set(world, pos.set(origin.getX() + x, origin.getY() + 2, origin.getZ() + z), Blocks.LANTERN.getDefaultState());
                }
            }
        }
        gate(world, origin, layout.startPillars(), layout.startWall(), Blocks.OBSIDIAN.getDefaultState(),
                Blocks.CRYING_OBSIDIAN.getDefaultState(), Blocks.CRYING_OBSIDIAN.getDefaultState());
        gate(world, origin, layout.endPillars(), layout.endWall(), Blocks.STONE_BRICKS.getDefaultState(),
                Blocks.CHISELED_STONE_BRICKS.getDefaultState(), Blocks.QUARTZ_BRICKS.getDefaultState());
    }

    /** Builds a gate: two pillars, a lintel between them and a back wall closing the path. */
    private static void gate(ServerWorld world, BlockPos origin, int[][] pillars, int[][] wall,
                             BlockState pillar, BlockState lintel, BlockState back) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int[] p : pillars) {
            for (int y = 1; y < GATE_HEIGHT; y++) {
                set(world, pos.set(origin.getX() + p[0], origin.getY() + y, origin.getZ() + p[1]), pillar);
            }
        }
        int[] a = pillars[0];
        int[] b = pillars[1];
        int steps = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
        for (int i = 0; i <= steps; i++) {
            int x = a[0] + Integer.signum(b[0] - a[0]) * i;
            int z = a[1] + Integer.signum(b[1] - a[1]) * i;
            set(world, pos.set(origin.getX() + x, origin.getY() + GATE_HEIGHT, origin.getZ() + z), lintel);
        }
        int midX = (a[0] + b[0]) / 2;
        int midZ = (a[1] + b[1]) / 2;
        set(world, pos.set(origin.getX() + midX, origin.getY() + GATE_HEIGHT + 1, origin.getZ() + midZ),
                Blocks.LANTERN.getDefaultState());
        for (int[] w : wall) {
            for (int y = 1; y <= GATE_HEIGHT; y++) {
                set(world, pos.set(origin.getX() + w[0], origin.getY() + y, origin.getZ() + w[1]), back);
            }
        }
    }

    /** Removes everything an arena consists of. */
    public static void clear(ServerWorld world, BlockPos origin, ArenaLayout layout) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = -1; x <= layout.size(); x++) {
            for (int z = -1; z <= layout.size(); z++) {
                for (int y = -CLEAR_BELOW; y <= CLEAR_ABOVE; y++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, air, FLAGS);
                    }
                }
            }
        }
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, FLAGS);
    }

    private static BlockState borderBrick(SplittableRandom random) {
        int r = random.nextInt(10);
        if (r == 0) {
            return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
        }
        if (r == 1) {
            return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        }
        return Blocks.STONE_BRICKS.getDefaultState();
    }

    private static BlockState plant(SplittableRandom random) {
        int r = random.nextInt(100);
        if (r < 6) {
            return Blocks.SHORT_GRASS.getDefaultState();
        }
        return switch (r) {
            case 6 -> Blocks.DANDELION.getDefaultState();
            case 7 -> Blocks.POPPY.getDefaultState();
            case 8 -> Blocks.CORNFLOWER.getDefaultState();
            case 9 -> Blocks.OXEYE_DAISY.getDefaultState();
            case 10 -> Blocks.AZURE_BLUET.getDefaultState();
            default -> null;
        };
    }
}
