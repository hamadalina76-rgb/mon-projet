package com.speedline.delivery.dispatch.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilTest {

    @Test
    void identicalPoints_zeroDistance() {
        assertEquals(0.0, GeoUtil.haversineMeters(36.8, 10.1, 36.8, 10.1), 0.0001);
    }

    @Test
    void tunisToSfax_isReasonable() {
        double meters = GeoUtil.haversineMeters(36.8065, 10.1815, 34.7406, 10.7603);
        assertTrue(meters > 200_000);
        assertTrue(meters < 260_000);
    }
}
