package io.github.dalpner.mctd.core;

/**
 * A polyline the creeps walk along. Positions are given as distance from the start; a lateral offset
 * shifts creeps sideways so that they do not all walk on the exact centre line. Around corners the
 * sideways offset and the facing direction are blended so creeps turn smoothly.
 */
public final class PathLine {
    private static final double BLEND = 1.5;

    private final double[] xs;
    private final double[] zs;
    private final double[] cumulative;
    private final double length;

    public PathLine(double[] xs, double[] zs) {
        if (xs.length != zs.length || xs.length < 2) {
            throw new IllegalArgumentException("A path needs at least two points");
        }
        this.xs = xs.clone();
        this.zs = zs.clone();
        this.cumulative = new double[xs.length];
        double sum = 0;
        for (int i = 1; i < xs.length; i++) {
            sum += Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
            cumulative[i] = sum;
        }
        this.length = sum;
    }

    public double length() {
        return length;
    }

    public int points() {
        return xs.length;
    }

    public double x(int i) {
        return xs[i];
    }

    public double z(int i) {
        return zs[i];
    }

    /**
     * Samples the path.
     *
     * @param distance distance from the start (clamped to the path)
     * @param lateral  sideways offset in blocks (positive = left of the walking direction)
     * @param out      receives {x, z, facingX, facingZ}
     */
    public void sample(double distance, double lateral, double[] out) {
        double d = Math.max(0, Math.min(length, distance));
        int last = xs.length - 2;
        int i = 0;
        while (i < last && cumulative[i + 1] <= d) {
            i++;
        }
        double segLen = cumulative[i + 1] - cumulative[i];
        double local = d - cumulative[i];
        double dx = (xs[i + 1] - xs[i]) / segLen;
        double dz = (zs[i + 1] - zs[i]) / segLen;
        double cx = xs[i] + dx * local;
        double cz = zs[i] + dz * local;
        double px = -dz;
        double pz = dx;
        double fx = dx;
        double fz = dz;
        if (i > 0 && local < BLEND) {
            double prevLen = cumulative[i] - cumulative[i - 1];
            double pdx = (xs[i] - xs[i - 1]) / prevLen;
            double pdz = (zs[i] - zs[i - 1]) / prevLen;
            double s = 0.5 + 0.5 * (local / BLEND);
            px = lerp(-pdz, px, s);
            pz = lerp(pdx, pz, s);
            fx = lerp(pdx, fx, s);
            fz = lerp(pdz, fz, s);
        } else if (i < last && segLen - local < BLEND) {
            double nextLen = cumulative[i + 2] - cumulative[i + 1];
            double ndx = (xs[i + 2] - xs[i + 1]) / nextLen;
            double ndz = (zs[i + 2] - zs[i + 1]) / nextLen;
            double s = 0.5 * (1.0 - (segLen - local) / BLEND);
            px = lerp(px, -ndz, s);
            pz = lerp(pz, ndx, s);
            fx = lerp(fx, ndx, s);
            fz = lerp(fz, ndz, s);
        }
        out[0] = cx + px * lateral;
        out[1] = cz + pz * lateral;
        out[2] = fx;
        out[3] = fz;
    }

    /** Length of the path that lies within {@code range} of the given point (approximation by sampling). */
    public double lengthWithin(double x, double z, double range) {
        double step = 0.25;
        double r2 = range * range;
        double[] tmp = new double[4];
        double covered = 0;
        for (double d = 0; d < length; d += step) {
            sample(d, 0, tmp);
            double ddx = tmp[0] - x;
            double ddz = tmp[1] - z;
            if (ddx * ddx + ddz * ddz <= r2) {
                covered += step;
            }
        }
        return covered;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
