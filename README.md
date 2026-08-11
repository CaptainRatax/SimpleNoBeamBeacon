# SimpleNoBeamBeacon

SimpleNoBeamBeacon is a server-side plugin built primarily for Paper 26.2. It keeps a beacon's effects active when tinted glass hides the native beam.

## Compatibility

| Artifact | Server | Minecraft | Java |
| --- | --- | --- | --- |
| `SimpleNoBeamBeacon-Paper-1.1.0.jar` | Paper | 26.2 (primary target) | 25 |
| The same JAR | Paper, Spigot, or compatible forks | 1.17.1 and newer (additional compatibility) | Whatever the server requires |

Paper 26.2 is the primary implementation and validation target. The release JAR nevertheless uses Java 16 bytecode, is compiled against the Bukkit 1.17.1 API, and declares `api-version: 1.17`. Its `Paper` filename describes the primary target; it is not a hard Paper dependency.

For Paper 26.2, the plugin dynamically reads the server's real light-dampening and beacon-selection data, without linking the JAR to version-specific classes. Older Bukkit-derived servers use the same bridge when their internal layout is recognised and otherwise fall back to the public Bukkit API. That fallback explicitly handles the important barrier, bedrock, slime-block, and tinted-glass mismatches, but unusual technical block states may still differ from vanilla on a server version whose internals are not recognised.

## Installation

1. Build or download `SimpleNoBeamBeacon-Paper-1.1.0.jar`.
2. Put it in the server's `plugins/` directory.
3. Restart the server.
4. Activate and configure the beacon before covering it with tinted glass.

No dependencies, client mods, or resource packs are required.

## Configuration

The plugin creates `plugins/SimpleNoBeamBeacon/config.yml` on first startup:

```yaml
enabled: true
```

Set `enabled` to `false` to stop hidden-beacon handling while keeping the plugin loaded and its administrative command available. Changes made directly to the file take effect after a server restart or `/simplenobeambeacon reload`.

Disabling the feature stops tracking and reapplying effects. Effects that were already applied expire naturally, so effects from potions or other beacons are not removed accidentally.

## Commands

| Command | Description |
| --- | --- |
| `/simplenobeambeacon reload` | Reloads `config.yml` and restarts the beacon service when enabled. |
| `/simplenobeambeacon set enabled` | Saves `enabled: true` and reloads the configuration immediately. |
| `/simplenobeambeacon set disabled` | Saves `enabled: false` and reloads the configuration immediately. |

`/snbb` is the short alias. `/snbb enable` and `/snbb disable` are also accepted as shortcuts. All commands are restricted to server operators and the server console.

## Behaviour

For a beacon column containing tinted glass, the plugin:

- ignores every tinted-glass block when deciding whether the beacon may apply effects;
- continues checking above the first tinted glass, so a later opaque block still disables the beacon effects;
- lets barrier and every other block accepted by the recognised vanilla obstruction rule pass, whether they are below or above the tinted glass;
- recalculates all four pyramid levels;
- preserves the selected primary effect, primary level II, regeneration, and other valid secondary effects on Paper 26.2;
- uses vanilla ranges and durations and respects a custom Paper effect range;
- reacts to block placement, breaking, pistons, explosions, chunk lifecycle, and periodic fallback scans.

On Paper 26.2, the obstruction check uses the same `BlockState#getLightDampening()` threshold and bedrock exception as the server's vanilla beacon scan. It deliberately treats tinted glass as transparent only for the invisible, effect-producing scan. It does not use Bukkit's unrelated `Material#isOccluding()` result when the exact server data is available, which fixes the barrier problem.

Removing the last tinted-glass block returns the beacon to normal vanilla effect handling. Placing an actual vanilla beam blocker anywhere in the column, including above tinted glass, stops replacement effects and lets already-applied effects expire normally.

## Visual limitation

A server-only plugin cannot make the native beam remain visible below tinted glass while hiding it above the glass for unmodified clients.

In Minecraft 26.2, the client scans the beacon column itself. When it reaches tinted glass, it clears the complete list of beam sections, including all coloured sections below that glass; the server does not send those sections in a packet. If the server instead presents tinted glass as transparent, the client renderer extends the final beam section towards the sky rather than ending it at that glass.

An exact visual implementation therefore requires a client mod that changes both the client-side column scan and renderer. Particles or display entities could only imitate the native beam, with different texture, animation, glow, colouring, render distance, and graphics-setting behaviour. This plugin keeps the server mechanics correct and does not add that inaccurate imitation.

## Building

JDK 25 is required to run the build because it includes a separate compile check against Paper 26.2:

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The server JAR is written to `bukkit/build/libs/`. Although JDK 25 runs the build, the resulting plugin classes target Java 16.

## Other limitations

- Reapplied effects do not fire Paper's `BeaconEffectEvent`; `EntityPotionEffectEvent` reports the plugin rather than a beacon as the cause.
- On an additional server version whose internal beacon-selection layout is not recognised, the public-API fallback can preserve a selection captured during an earlier refresh. After a restart, temporarily uncovering and reactivating such a beacon may be necessary to recover secondary or level-II selection data.
- A future server version can change internal layouts. In that case the JAR still avoids a linkage crash and uses its compatibility fallback, but that fallback may not reproduce every unusual vanilla block state exactly.

## License

[MIT](LICENSE)
