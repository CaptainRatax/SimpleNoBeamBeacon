package com.captainratax.simplenobeambeacon;

import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleNoBeamBeaconPlugin extends JavaPlugin {
    private HiddenBeaconService service;

    @Override
    public void onEnable() {
        CompatibleScheduler scheduler = new CompatibleScheduler(this);
        service = new HiddenBeaconService(this, scheduler);
        service.start();

        getLogger().info("Enabled on " + (scheduler.isFolia() ? "Folia" : "Bukkit/Paper")
                + "; tinted glass above beacons now hides their beam without losing effects.");
    }

    @Override
    public void onDisable() {
        if (service != null) {
            int tracked = service.trackedCount();
            service.stop();
            getLogger().info("Disabled; stopped tracking " + tracked + " hidden beacon(s).");
        }
    }
}
