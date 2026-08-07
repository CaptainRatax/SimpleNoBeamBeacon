package com.captainratax.simplenobeambeacon;

import org.bukkit.Location;
import org.bukkit.entity.Player;

interface SchedulerFacade {
    @FunctionalInterface
    interface RepeatingTask {
        boolean run();
    }

    void runAtLocation(Location location, long initialDelayTicks, long periodTicks, RepeatingTask task);

    void runForPlayer(
            Player player,
            long initialDelayTicks,
            long periodTicks,
            RepeatingTask task,
            Runnable retired
    );

    void runLater(Location location, long delayTicks, Runnable task);

    boolean isFolia();
}
