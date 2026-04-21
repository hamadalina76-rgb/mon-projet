package com.speedline.delivery.dispatch.impl.solver;

import java.util.Arrays;

/**
 * Pure-Java Kuhn-Munkres (Hungarian) implementation for optimal min-cost assignment.
 *
 * <p>Inputs must be a <strong>square</strong> N×N matrix of non-negative finite costs.
 * Callers (see {@link HungarianSolver}) are responsible for padding rectangular inputs
 * with {@link #INF_COST_PLACEHOLDER} to signal infeasible pairs.
 *
 * <p>Complexity: O(N^3). For N=10 this is ~1000 ops and completes in well under 200 ms
 * even on modest hardware.
 */
public final class HungarianAlgorithm {

    /** Sentinel used by the wrapper to mark infeasible pairs; must stay below {@link Double#MAX_VALUE}/2. */
    public static final double INF_COST_PLACEHOLDER = 1.0e12;

    private HungarianAlgorithm() {}

    /**
     * Solve the assignment problem.
     *
     * @param cost square cost matrix (cost[i][j] = cost of assigning row i to col j)
     * @return {@code rowToCol[i]} = column assigned to row i
     */
    public static int[] solve(double[][] cost) {
        if (cost == null || cost.length == 0) return new int[0];
        final int n = cost.length;
        for (double[] row : cost) {
            if (row.length != n) {
                throw new IllegalArgumentException("HungarianAlgorithm requires a square matrix");
            }
        }

        // 1-based working arrays (index 0 unused) — classic Kuhn-Munkres formulation.
        final double[] u = new double[n + 1];
        final double[] v = new double[n + 1];
        final int[] p = new int[n + 1];
        final int[] way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            final double[] minv = new double[n + 1];
            final boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);

            do {
                used[j0] = true;
                final int i0 = p[j0];
                double delta = Double.POSITIVE_INFINITY;
                int j1 = -1;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) continue;
                    final double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }
                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }

                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }

                j0 = j1;
            } while (p[j0] != 0);

            do {
                final int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        final int[] rowToCol = new int[n];
        for (int j = 1; j <= n; j++) {
            if (p[j] != 0) rowToCol[p[j] - 1] = j - 1;
        }
        return rowToCol;
    }
}
