package com.captainratax.simplenobeambeacon;

import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

final class HiddenBeacon {
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;
    private final int tier;
    private final double range;
    private final PotionEffectType primary;
    private final PotionEffectType secondary;
    private final boolean upgradedPrimary;
    private final Object generation;

    HiddenBeacon(
            UUID worldId,
            int x,
            int y,
            int z,
            int tier,
            double range,
            PotionEffectType primary,
            PotionEffectType secondary,
            boolean upgradedPrimary,
            Object generation
    ) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
        this.range = range;
        this.primary = primary;
        this.secondary = secondary;
        this.upgradedPrimary = upgradedPrimary;
        this.generation = generation;
    }

    UUID worldId() {
        return worldId;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    int z() {
        return z;
    }

    int tier() {
        return tier;
    }

    double range() {
        return range;
    }

    PotionEffectType primary() {
        return primary;
    }

    PotionEffectType secondary() {
        return secondary;
    }

    boolean upgradedPrimary() {
        return upgradedPrimary;
    }

    Object generation() {
        return generation;
    }
}
