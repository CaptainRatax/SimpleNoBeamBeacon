package com.captainratax.simplenobeambeacon;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

final class FeatureConfiguration {
    private static final String ENABLED_PATH = "enabled";

    private FeatureConfiguration() {
    }

    static boolean loadEnabled(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        if (!configuration.contains(ENABLED_PATH)) {
            return true;
        }
        if (!configuration.isBoolean(ENABLED_PATH)) {
            throw new InvalidConfigurationException(
                    "Config value 'enabled' must be true or false."
            );
        }
        return configuration.getBoolean(ENABLED_PATH);
    }

    static void saveEnabled(File file, boolean enabled) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        if (file.isFile()) {
            try {
                configuration.load(file);
            } catch (InvalidConfigurationException ignored) {
                configuration = new YamlConfiguration();
            }
        }
        configuration.options().header(
                "Enables or disables hidden-beacon handling while keeping the plugin loaded."
        );
        configuration.set(ENABLED_PATH, enabled);
        configuration.save(file);
    }
}
