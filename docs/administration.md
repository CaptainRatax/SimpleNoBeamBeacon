# Configuration and commands

## Configuration

The plugin creates `plugins/SimpleNoBeamBeacon/config.yml` with one global setting:

```yaml
# Enables or disables hidden-beacon handling while keeping the plugin loaded.
enabled: true
```

| YAML path | Type | Default | Allowed values | Effect | Apply after a manual edit |
| --- | --- | --- | --- | --- | --- |
| `enabled` | Boolean | `true` | `true`, `false` | Starts or stops tracking and replacement effects for all qualifying beacons | Run `/simplenobeambeacon reload` or restart the server |

Setting `enabled: false` does not unload the plugin and does not remove its administrative command. It stops beacon tracking and future effect reapplication. Effects already applied by the plugin are not forcibly removed; they expire naturally.

If `enabled` is missing, the plugin treats it as `true`. Use the Boolean values `true` or `false`, not text such as `enabled` or `sometimes`.

### Invalid configuration

During `/simplenobeambeacon reload`, malformed YAML or a non-Boolean `enabled` value is rejected and the current running state is kept. The enable and disable commands can rewrite and repair an invalid file.

If the file cannot be read during server startup, the plugin logs a warning and uses the default enabled state.

## Commands

The primary command is `/simplenobeambeacon`. Every form also works with the `/snbb` alias, and arguments are case-insensitive.

| Syntax | Description | Player | Console |
| --- | --- | --- | --- |
| `/simplenobeambeacon` | Shows the current state and usage because no action was supplied | Operator only | Yes |
| `/simplenobeambeacon reload` | Validates and reloads `config.yml`, then restarts this plugin's beacon service when enabled | Operator only | Yes |
| `/simplenobeambeacon set enabled` | Writes `enabled: true` and applies it immediately | Operator only | Yes |
| `/simplenobeambeacon set disabled` | Writes `enabled: false` and applies it immediately | Operator only | Yes |
| `/simplenobeambeacon enable` | Shortcut for `set enabled` | Operator only | Yes |
| `/simplenobeambeacon disable` | Shortcut for `set disabled` | Operator only | Yes |

Examples:

```text
/snbb reload
/snbb disable
/snbb set enabled
```

The reload subcommand reloads only this plugin's configuration and service. It does not reload the plugin JAR or the Minecraft server.

## Permission

| Permission | Default | Purpose |
| --- | --- | --- |
| `simplenobeambeacon.admin` | `op` | Allows use of the administrative command declared in `plugin.yml` |

The command handler also explicitly requires the sender to be a server operator. Granting `simplenobeambeacon.admin` to a non-operator is not sufficient by itself. Non-operators receive no command tab completions.

There is no permission for receiving, bypassing, or toggling beacon effects. When the feature is enabled, it applies globally to eligible players in range of every qualifying beacon.
