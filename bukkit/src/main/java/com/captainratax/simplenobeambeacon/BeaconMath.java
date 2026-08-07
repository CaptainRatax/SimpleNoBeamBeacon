package com.captainratax.simplenobeambeacon;

final class BeaconMath {
    private BeaconMath() {
    }

    static int rangeForTier(int tier) {
        return tier * 10 + 10;
    }

    static int effectDurationTicks(int tier) {
        return (9 + tier * 2) * 20;
    }

    static boolean intersects(
            int beaconX,
            int beaconY,
            int beaconZ,
            double range,
            double worldHeight,
            double entityMinX,
            double entityMinY,
            double entityMinZ,
            double entityMaxX,
            double entityMaxY,
            double entityMaxZ
    ) {
        return entityMaxX > beaconX - range
                && entityMinX < beaconX + 1.0D + range
                && entityMaxZ > beaconZ - range
                && entityMinZ < beaconZ + 1.0D + range
                && entityMaxY > beaconY - range
                && entityMinY < beaconY + 1.0D + range + worldHeight;
    }
}
