# Troubleshooting

## The plugin does not load

Check the following:

1. Install `SimpleNoBeamBeacon-Paper-1.1.0.jar`, not the `-sources.jar` file.
2. Put the JAR in the server's `plugins/` directory, not a client or Sponge `mods/` directory.
3. Use Paper 26.2 with Java 25 for the primary target. For the additional 1.17.1+ compatibility range, use the Java version required by that server and never less than Java 16 for this plugin's bytecode.
4. Do not run duplicate SimpleNoBeamBeacon versions at the same time.
5. Check the startup log for the first exception involving `SimpleNoBeamBeacon`.

The plugin has no mandatory plugin dependency. A missing-dependency error therefore suggests that the wrong artifact was installed or that the error belongs to another plugin.

## The beam is still visible or looks different

SimpleNoBeamBeacon does not hide beams itself. Confirm that the block directly in the beacon's X/Z column is **tinted glass**, not ordinary glass or stained glass.

On an unmodified Minecraft 26.2 client, tinted glass removes the entire native beam. Client mods, shaders, and other rendering changes are outside this server plugin's control. Compare with an unmodified client before reporting a beam-rendering problem.

## The beam is hidden but effects are missing

Verify each condition:

- `enabled` is a Boolean `true` in `plugins/SimpleNoBeamBeacon/config.yml`;
- the beacon has at least one complete valid pyramid level;
- a primary power was selected while the beacon was uncovered;
- the tinted glass is in the same vertical column as the beacon;
- no genuine beam blocker exists anywhere above the beacon, including above the tinted glass;
- the player is not in spectator mode and is inside the beacon's range; and
- the beacon's chunk is loaded.

Allow one 80-tick refresh cycle—about four seconds at 20 TPS—after changing the beacon. On a non-primary server version whose internal beacon selection cannot be read after a restart, temporarily remove the tinted glass, reactivate and configure the beacon, then cover it again.

## Effects remain briefly after disabling or blocking a beacon

This is expected. The plugin does not forcibly remove potion effects because the same effect may also come from a potion or another beacon. The last plugin application expires naturally after 11, 13, 15, or 17 seconds depending on the pyramid tier.

## Configuration reload is rejected

Use valid YAML and a real Boolean:

```yaml
enabled: true
```

Values such as `enabled: sometimes` are invalid. A rejected reload leaves the current state unchanged. An operator or the server console can run `/snbb enable` or `/snbb disable` to rewrite the setting and apply it immediately.

## A block change was not detected

Placement, breaking, physics, pistons, explosions, chunk lifecycle, and beacon interaction are handled. Tracked beacons also refresh every 80 ticks. However, there is no repeating whole-world discovery scan. If another plugin changes blocks without the relevant Bukkit events, try interacting with the beacon, reloading the plugin configuration, unloading and loading the chunk, or restarting the server.

## Another plugin treats the effects differently

Replacement effects are normal potion effects applied by SimpleNoBeamBeacon. They do not fire Paper's `BeaconEffectEvent`, and `EntityPotionEffectEvent` identifies the plugin rather than a beacon as the cause. A plugin that relies on native beacon event attribution may therefore behave differently.

No named plugin integrations or conflicts are declared by this project. If the problem remains, reproduce it without other beacon or potion-effect plugins and open a [GitHub issue](https://github.com/CaptainRatax/SimpleNoBeamBeacon/issues) with the SimpleNoBeamBeacon version, server software and build, Minecraft version, Java version, relevant configuration, and startup log.
