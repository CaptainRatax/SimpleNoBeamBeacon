package com.captainratax.simplenobeambeacon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeaconMathTest {
    @Test
    void usesVanillaRangesForAllTiers() {
        assertEquals(20, BeaconMath.rangeForTier(1));
        assertEquals(30, BeaconMath.rangeForTier(2));
        assertEquals(40, BeaconMath.rangeForTier(3));
        assertEquals(50, BeaconMath.rangeForTier(4));
    }

    @Test
    void usesVanillaEffectDurationsForAllTiers() {
        assertEquals(220, BeaconMath.effectDurationTicks(1));
        assertEquals(260, BeaconMath.effectDurationTicks(2));
        assertEquals(300, BeaconMath.effectDurationTicks(3));
        assertEquals(340, BeaconMath.effectDurationTicks(4));
    }

    @Test
    void rangeIntersectsEntityBoundingBoxesLikeVanilla() {
        assertTrue(BeaconMath.intersects(
                10, 64, 10, 20, 384,
                -10.3D, 43.0D, -10.3D,
                -9.7D, 44.8D, -9.7D
        ));
        assertTrue(BeaconMath.intersects(
                10, 64, 10, 20, 384,
                30.8D, 300.0D, 30.8D,
                31.4D, 301.8D, 31.4D
        ));

        assertFalse(BeaconMath.intersects(
                10, 64, 10, 20, 384,
                -10.6D, 64.0D, 9.7D,
                -10.01D, 65.8D, 10.3D
        ));
        assertFalse(BeaconMath.intersects(
                10, 64, 10, 20, 384,
                31.01D, 64.0D, 9.7D,
                31.6D, 65.8D, 10.3D
        ));
        assertFalse(BeaconMath.intersects(
                10, 64, 10, 20, 384,
                9.7D, 42.0D, 9.7D,
                10.3D, 44.0D, 10.3D
        ));
        assertFalse(BeaconMath.intersects(
                10, 64, 10, 20, 384,
                9.7D, 469.0D, 9.7D,
                10.3D, 470.8D, 10.3D
        ));
    }
}
