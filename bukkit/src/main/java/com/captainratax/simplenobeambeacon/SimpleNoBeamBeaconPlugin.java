package com.captainratax.simplenobeambeacon;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class SimpleNoBeamBeaconPlugin extends JavaPlugin {
    private HiddenBeaconService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerAdminCommand();

        Boolean configured = loadConfiguredEnabled();
        boolean enabled = configured == null || configured;
        applyServiceState(enabled);
        getLogger().info("Loaded with beacon handling " + stateName(enabled) + ".");
    }

    @Override
    public void onDisable() {
        stopService();
    }

    ConfigurationResult reloadFeatureConfiguration() {
        Boolean configured = loadConfiguredEnabled();
        if (configured == null) {
            return ConfigurationResult.invalid(isFeatureEnabled());
        }
        reloadConfig();
        stopService();
        applyServiceState(configured);
        return ConfigurationResult.success(configured);
    }

    ConfigurationResult updateFeatureConfiguration(boolean enabled) {
        try {
            FeatureConfiguration.saveEnabled(configFile(), enabled);
        } catch (IOException exception) {
            getLogger().warning("Could not save config.yml: " + exception.getMessage());
            return ConfigurationResult.invalid(isFeatureEnabled());
        }
        return reloadFeatureConfiguration();
    }

    boolean isFeatureEnabled() {
        return service != null;
    }

    private void registerAdminCommand() {
        PluginCommand command = getCommand("simplenobeambeacon");
        if (command == null) {
            throw new IllegalStateException("Command simplenobeambeacon is missing from plugin.yml");
        }
        PluginAdminCommand handler = new PluginAdminCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private Boolean loadConfiguredEnabled() {
        try {
            return FeatureConfiguration.loadEnabled(configFile());
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            getLogger().warning("Could not read config.yml; keeping the current or default state: "
                    + exception.getMessage());
            return null;
        }
    }

    private File configFile() {
        return new File(getDataFolder(), "config.yml");
    }

    private void applyServiceState(boolean enabled) {
        if (!enabled || service != null) {
            return;
        }
        CompatibleScheduler scheduler = new CompatibleScheduler(this);
        service = new HiddenBeaconService(this, scheduler);
        service.start();
    }

    private void stopService() {
        HiddenBeaconService current = service;
        service = null;
        if (current == null) {
            return;
        }
        int tracked = current.trackedCount();
        current.stop();
        getLogger().info("Stopped tracking " + tracked + " hidden beacon(s).");
    }

    private static String stateName(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    static final class ConfigurationResult {
        private final boolean valid;
        private final boolean enabled;

        private ConfigurationResult(boolean valid, boolean enabled) {
            this.valid = valid;
            this.enabled = enabled;
        }

        static ConfigurationResult success(boolean enabled) {
            return new ConfigurationResult(true, enabled);
        }

        static ConfigurationResult invalid(boolean enabled) {
            return new ConfigurationResult(false, enabled);
        }

        boolean valid() {
            return valid;
        }

        boolean enabled() {
            return enabled;
        }
    }
}
