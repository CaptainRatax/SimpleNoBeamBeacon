package com.captainratax.simplenobeambeacon;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

final class BeaconKey {
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;

    private BeaconKey(UUID worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static BeaconKey from(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return new BeaconKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
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

    int chunkX() {
        return x >> 4;
    }

    int chunkZ() {
        return z >> 4;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeaconKey)) {
            return false;
        }
        BeaconKey that = (BeaconKey) other;
        return x == that.x && y == that.y && z == that.z && worldId.equals(that.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, x, y, z);
    }
}
