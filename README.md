# SimpleNoBeamBeacon

A small server-side plugin that lets tinted glass hide a beacon beam without disabling the beacon's effects. Players do not need to install a mod or resource pack.

## Compatibility

| Artifact | Platforms | Minecraft | Server Java |
| --- | --- | --- | --- |
| `SimpleNoBeamBeacon-Bukkit-1.0.0.jar` | Paper, Spigot, Bukkit/CraftBukkit, and Purpur | 1.17–26.2 | Whatever the server version requires |
| The same Bukkit JAR | Folia | Folia versions based on 1.19.4–26.2 | Whatever the server version requires |
| `SimpleNoBeamBeacon-Sponge-26.2-1.0.0.jar` | Experimental SpongeVanilla | 26.2 | Java 25 |

Minecraft 1.17 is the oldest supported version because it introduced tinted glass. The Bukkit JAR is compiled against Spigot API 1.17.1 as Java 16 bytecode and is also checked against Paper 26.2.

Sponge uses a different API and plugin loader, so it requires a separate JAR. Sponge 26.2/API 20 is still experimental at the time of this release.

## Installation

1. Download the JAR for your server.
2. Paper, Spigot, Bukkit, Purpur, or Folia: place the Bukkit JAR in `plugins/`.
3. SpongeVanilla 26.2: place the Sponge JAR directly in `mods/`, not `mods/plugins/`, because it contains a Mixin.
4. Restart the server.
5. Select the beacon effect and let the beacon activate at least once. Then place tinted glass anywhere in the vertical column above the beacon.

There are no commands, permissions, or required configuration files.

## Implementation

Tinted glass already hides the beam on the client, but vanilla Minecraft also treats it as an obstruction on the server and stops applying beacon effects. Bukkit does not provide a public API for changing only that obstruction rule.

The Bukkit build tracks covered beacons and reapplies their effects using vanilla range, duration, pyramid, and effect-level rules. It also handles chunk and block changes, piston movement, world lifecycle, and Folia's region-safe schedulers without relying on version-specific server internals.

The Sponge build uses a server-side Mixin to change the exact obstruction check. The client still sees real tinted glass and hides the beam, while the server keeps native beacon behavior.

## Behavior

The Bukkit build:

- recalculates all four pyramid levels;
- uses vanilla ranges of 20, 30, 40, or 50 blocks;
- uses vanilla effect durations and refreshes every 80 ticks;
- supports primary effect level II and regeneration as a secondary effect;
- respects custom Paper effect ranges;
- stops applying effects if the pyramid, beacon, tinted glass, or sky access becomes invalid;
- uses region and entity schedulers on Folia.

Removing the tinted glass immediately restores fully vanilla behavior.

## Building

JDK 25 is required to build and validate the 26.2 variant:

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The JARs are written to `bukkit/build/libs/` and `sponge/build/libs/`.

## Known limitations

- For compatibility back to 1.17, the Bukkit JAR uses only public Bukkit API. Reapplied effects do not fire Paper's `BeaconEffectEvent`, and `EntityPotionEffectEvent` reports the plugin rather than a beacon as the cause.
- Bukkit does not expose the exact light opacity of every block state. Normal blocks behave as expected, but a few unusual technical block states may differ from vanilla obstruction behavior.
- Bukkit does not expose a secondary selection before a new beacon has activated. Let a new beacon apply its selected effect once before covering it, especially when using regeneration or primary effect level II.
- A server configured with `minimum-api` above 1.17 in `bukkit.yml` will reject plugins that declare the older API level.
- The Sponge JAR targets Minecraft 26.2 internals and must not be used on another version.

## License

[MIT](LICENSE)
