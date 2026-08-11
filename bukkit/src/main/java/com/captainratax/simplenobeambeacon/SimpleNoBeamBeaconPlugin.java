package com.captainratax.simplenobeambeacon;

import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleNoBeamBeaconPlugin extends JavaPlugin {
    private HiddenBeaconService service;

    @Override
    public void onEnable() {
        CompatibleScheduler scheduler = new CompatibleScheduler(this);
        service = new HiddenBeaconService(this, scheduler);
        service.start();

        getLogger().info("Enabled; tinted glass now stops the visible beam without losing beacon effects.");
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
