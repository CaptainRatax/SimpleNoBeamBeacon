package com.captainratax.simplenobeambeacon;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginAdminCommandTest {
    @Test
    void parsesReloadAndDirectStateShortcutsCaseInsensitively() {
        assertEquals(
                PluginAdminCommand.Action.RELOAD,
                PluginAdminCommand.parseAction(new String[]{"ReLoAd"})
        );
        assertEquals(
                PluginAdminCommand.Action.ENABLE,
                PluginAdminCommand.parseAction(new String[]{"ENABLE"})
        );
        assertEquals(
                PluginAdminCommand.Action.DISABLE,
                PluginAdminCommand.parseAction(new String[]{"disable"})
        );
    }

    @Test
    void parsesSetEnabledAndDisabled() {
        assertEquals(
                PluginAdminCommand.Action.ENABLE,
                PluginAdminCommand.parseAction(new String[]{"set", "enabled"})
        );
        assertEquals(
                PluginAdminCommand.Action.DISABLE,
                PluginAdminCommand.parseAction(new String[]{"SET", "DISABLED"})
        );
    }

    @Test
    void invalidArgumentsShowHelp() {
        assertEquals(
                PluginAdminCommand.Action.HELP,
                PluginAdminCommand.parseAction(new String[0])
        );
        assertEquals(
                PluginAdminCommand.Action.HELP,
                PluginAdminCommand.parseAction(new String[]{"set", "maybe"})
        );
        assertEquals(
                PluginAdminCommand.Action.HELP,
                PluginAdminCommand.parseAction(new String[]{"reload", "extra"})
        );
    }

    @Test
    void packagedConfigEnablesTheFeatureByDefault() {
        YamlConfiguration config = loadResource("/config.yml");
        assertTrue(config.isBoolean("enabled"));
        assertTrue(config.getBoolean("enabled"));
    }

    @Test
    void rejectsMalformedOrNonBooleanConfiguration(@TempDir Path directory) throws Exception {
        Path malformed = directory.resolve("malformed.yml");
        Files.writeString(malformed, "enabled: [", StandardCharsets.UTF_8);
        assertThrows(
                org.bukkit.configuration.InvalidConfigurationException.class,
                () -> FeatureConfiguration.loadEnabled(malformed.toFile())
        );

        Path wrongType = directory.resolve("wrong-type.yml");
        Files.writeString(wrongType, "enabled: sometimes\n", StandardCharsets.UTF_8);
        assertThrows(
                org.bukkit.configuration.InvalidConfigurationException.class,
                () -> FeatureConfiguration.loadEnabled(wrongType.toFile())
        );

        FeatureConfiguration.saveEnabled(malformed.toFile(), false);
        assertEquals(false, FeatureConfiguration.loadEnabled(malformed.toFile()));
    }

    @Test
    void missingEnabledSettingUsesTheBackwardsCompatibleDefault(@TempDir Path directory)
            throws Exception {
        Path empty = directory.resolve("empty.yml");
        Files.writeString(empty, "# no setting\n", StandardCharsets.UTF_8);
        assertTrue(FeatureConfiguration.loadEnabled(empty.toFile()));
    }

    @Test
    void packagedPluginMetadataRestrictsTheAdminCommandToOps() {
        YamlConfiguration metadata = loadResource("/plugin.yml");
        assertEquals("snbb", metadata.getStringList(
                "commands.simplenobeambeacon.aliases"
        ).get(0));
        assertEquals(
                "simplenobeambeacon.admin",
                metadata.getString("commands.simplenobeambeacon.permission")
        );
        assertEquals(
                "op",
                metadata.getString("permissions.simplenobeambeacon.admin.default")
        );
    }

    private static YamlConfiguration loadResource(String name) {
        InputStream stream = PluginAdminCommandTest.class.getResourceAsStream(name);
        assertNotNull(stream, name);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new AssertionError("Could not load " + name, exception);
        }
    }
}
