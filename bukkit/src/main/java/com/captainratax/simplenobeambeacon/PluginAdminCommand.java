package com.captainratax.simplenobeambeacon;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class PluginAdminCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(
            Arrays.asList("reload", "set", "enable", "disable")
    );
    private static final List<String> SET_VALUES = Collections.unmodifiableList(
            Arrays.asList("enabled", "disabled")
    );

    private final SimpleNoBeamBeaconPlugin plugin;

    PluginAdminCommand(SimpleNoBeamBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] arguments
    ) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only server operators can use this command.");
            return true;
        }

        Action action = parseAction(arguments);
        switch (action) {
            case RELOAD:
                SimpleNoBeamBeaconPlugin.ConfigurationResult result =
                        plugin.reloadFeatureConfiguration();
                if (!result.valid()) {
                    sender.sendMessage(ChatColor.RED
                            + "The configuration was not reloaded because 'enabled' is invalid.");
                    return true;
                }
                sendState(sender, "Configuration reloaded", result.enabled());
                return true;
            case ENABLE:
                updateState(sender, true);
                return true;
            case DISABLE:
                updateState(sender, false);
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "Beacon handling is currently "
                        + stateName(plugin.isFeatureEnabled()) + ".");
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label
                        + " <reload|set <enabled|disabled>>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] arguments
    ) {
        if (!sender.isOp()) {
            return Collections.emptyList();
        }

        if (arguments.length == 1) {
            return matching(SUBCOMMANDS, arguments[0]);
        }
        if (arguments.length == 2 && arguments[0].equalsIgnoreCase("set")) {
            return matching(SET_VALUES, arguments[1]);
        }
        return Collections.emptyList();
    }

    private static List<String> matching(List<String> choices, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String choice : choices) {
            if (choice.startsWith(prefix)) {
                matches.add(choice);
            }
        }
        return matches;
    }

    static Action parseAction(String[] arguments) {
        if (arguments.length == 2 && arguments[0].equalsIgnoreCase("set")) {
            if (arguments[1].equalsIgnoreCase("enabled")) {
                return Action.ENABLE;
            }
            if (arguments[1].equalsIgnoreCase("disabled")) {
                return Action.DISABLE;
            }
        }
        if (arguments.length != 1) {
            return Action.HELP;
        }
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                return Action.RELOAD;
            case "enable":
                return Action.ENABLE;
            case "disable":
                return Action.DISABLE;
            default:
                return Action.HELP;
        }
    }

    private void updateState(CommandSender sender, boolean requestedState) {
        SimpleNoBeamBeaconPlugin.ConfigurationResult result =
                plugin.updateFeatureConfiguration(requestedState);
        if (!result.valid() || result.enabled() != requestedState) {
            sender.sendMessage(ChatColor.RED
                    + "The configuration could not be changed to the requested state.");
            return;
        }
        sendState(sender, "Configuration updated and reloaded", result.enabled());
    }

    private static void sendState(CommandSender sender, String action, boolean enabled) {
        sender.sendMessage(ChatColor.GREEN + action + ". Beacon handling is now "
                + stateName(enabled) + ".");
    }

    private static String stateName(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    enum Action {
        RELOAD,
        ENABLE,
        DISABLE,
        HELP
    }
}
