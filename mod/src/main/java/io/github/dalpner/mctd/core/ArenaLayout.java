package io.github.dalpner.mctd.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena maps. Each layout is a square of {@code size} x {@code size} cells with a path of width three
 * running through the given waypoints. All cells that are neither path, border nor decoration can be
 * built on.
 */
public enum ArenaLayout {
    /** Four long lanes connected like a snake. */
    SERPENTINE("serpentine", 49, new int[][]{
            {1, 6}, {45, 6}, {45, 18}, {4, 18}, {4, 30}, {45, 30}, {45, 42}, {1, 42}}),
    /** A spiral towards the centre; the longest path. */
    SPIRAL("spiral", 49, new int[][]{
            {1, 4}, {44, 4}, {44, 44}, {4, 44}, {4, 12}, {36, 12}, {36, 36}, {12, 36},
            {12, 20}, {28, 20}, {28, 28}, {20, 28}}),
    /** Three far-apart vertical lanes; the shortest path and the hardest map. */
    ZIGZAG("zigzag", 49, new int[][]{
            {4, 1}, {4, 44}, {24, 44}, {24, 4}, {44, 4}, {44, 47}});

    public static final byte BUILD = 0;
    public static final byte PATH = 1;
    public static final byte BORDER = 2;
    public static final byte DECOR = 3;

    private final String id;
    private final int size;
    private final int[][] waypoints;
    private final byte[] grid;
    private final PathLine path;
    private final int[][] startPillars;
    private final int[][] endPillars;

    ArenaLayout(String id, int size, int[][] waypoints) {
        this.id = id;
        this.size = size;
        this.waypoints = waypoints;
        this.grid = new byte[size * size];
        double[] xs = new double[waypoints.length];
        double[] zs = new double[waypoints.length];
        for (int i = 0; i < waypoints.length; i++) {
            xs[i] = waypoints[i][0] + 0.5;
            zs[i] = waypoints[i][1] + 0.5;
        }
        this.path = new PathLine(xs, zs);
        this.startPillars = pillars(waypoints[0], waypoints[1]);
        this.endPillars = pillars(waypoints[waypoints.length - 1], waypoints[waypoints.length - 2]);
        buildGrid();
    }

    /** Two cells beside the path at {@code at}, perpendicular to the direction towards {@code other}. */
    private static int[][] pillars(int[] at, int[] other) {
        int dx = Integer.signum(other[0] - at[0]);
        int dz = Integer.signum(other[1] - at[1]);
        return new int[][]{{at[0] - 2 * dz, at[1] + 2 * dx}, {at[0] + 2 * dz, at[1] - 2 * dx}};
    }

    private void buildGrid() {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (x == 0 || z == 0 || x == size - 1 || z == size - 1) {
                    grid[x * size + z] = BORDER;
                }
            }
        }
        for (int i = 0; i + 1 < waypoints.length; i++) {
            int[] a = waypoints[i];
            int[] b = waypoints[i + 1];
            int minX = Math.min(a[0], b[0]) - 1;
            int maxX = Math.max(a[0], b[0]) + 1;
            int minZ = Math.min(a[1], b[1]) - 1;
            int maxZ = Math.max(a[1], b[1]) + 1;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (inside(x, z) && grid[x * size + z] != BORDER) {
                        grid[x * size + z] = PATH;
                    }
                }
            }
        }
        for (int[] p : startPillars) {
            markDecor(p[0], p[1]);
        }
        for (int[] p : endPillars) {
            markDecor(p[0], p[1]);
        }
    }

    private void markDecor(int x, int z) {
        if (inside(x, z) && grid[x * size + z] == BUILD) {
            grid[x * size + z] = DECOR;
        }
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mctd.layout." + id;
    }

    public int size() {
        return size;
    }

    public PathLine path() {
        return path;
    }

    public int[][] waypoints() {
        return waypoints;
    }

    public int[][] startPillars() {
        return startPillars;
    }

    public int[][] endPillars() {
        return endPillars;
    }

    public boolean inside(int x, int z) {
        return x >= 0 && z >= 0 && x < size && z < size;
    }

    public byte cell(int x, int z) {
        return inside(x, z) ? grid[x * size + z] : BORDER;
    }

    public boolean isBuildable(int x, int z) {
        return cell(x, z) == BUILD;
    }

    public boolean isPath(int x, int z) {
        return cell(x, z) == PATH;
    }

    /** All buildable cells as {x, z} pairs. */
    public List<int[]> buildableCells() {
        List<int[]> cells = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (grid[x * size + z] == BUILD) {
                    cells.add(new int[]{x, z});
                }
            }
        }
        return cells;
    }

    /** A buildable cell close to the centre where players arrive. */
    public int[] playerSpawn() {
        int c = size / 2;
        int[] best = {c, c};
        double bestDist = Double.MAX_VALUE;
        for (int x = 1; x < size - 1; x++) {
            for (int z = 1; z < size - 1; z++) {
                if (grid[x * size + z] != BUILD) {
                    continue;
                }
                double d = (x - c) * (x - c) + (z - c) * (z - c);
                if (d < bestDist) {
                    bestDist = d;
                    best = new int[]{x, z};
                }
            }
        }
        return best;
    }

    public static ArenaLayout byId(String id) {
        for (ArenaLayout l : values()) {
            if (l.id.equalsIgnoreCase(id)) {
                return l;
            }
        }
        return null;
    }
}
