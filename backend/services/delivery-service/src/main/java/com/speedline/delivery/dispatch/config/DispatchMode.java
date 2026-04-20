package com.speedline.delivery.dispatch.config;

/**
 * DISP-101: per-zone dispatch strategy.
 * <ul>
 *   <li>{@link #AUTO}: solver output is published as firm assignments.</li>
 *   <li>{@link #SEMI_AUTO}: solver runs, assignments are published as proposals (requires operator confirmation).</li>
 *   <li>{@link #MANUAL}: scheduler skips the zone entirely (operator dispatches by hand).</li>
 * </ul>
 */
public enum DispatchMode {
    AUTO,
    SEMI_AUTO,
    MANUAL
}
