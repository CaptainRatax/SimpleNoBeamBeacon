# How it works

## Core behavior

SimpleNoBeamBeacon does not directly hide a beacon beam. Instead, a real tinted-glass block in the beacon's vertical column causes the normal client to discard the native beam. The plugin detects that specific arrangement and applies replacement potion effects when the beacon would otherwise be valid.

The implementation does not replace blocks, send beam packets, or create a particle or display-entity beam. It reads the beacon's selected powers and applies `PotionEffect` instances to eligible players.

A hidden beacon produces replacement effects only when all of these conditions are true:

| Condition | Behavior |
| --- | --- |
| Global feature | `enabled` is `true` |
| Tinted glass | At least one tinted-glass block is directly above the beacon in the same X/Z column |
| Pyramid | At least one complete level is made from blocks in the server's beacon-base tag |
| Selected power | A primary beacon power can be read |
| Beam column | No real beam-blocking block exists anywhere above the beacon, including above the tinted glass |
| Player | The player is online, is not in spectator mode, is in the same world, and intersects the effect range |

Other beacons are not changed. There is no per-player or per-beacon toggle: the global setting applies to every beacon that matches these conditions.

## Effects and beacon levels

The plugin recalculates all four pyramid levels instead of relying on the inactive beacon's reported level. It preserves a readable primary selection, tier-4 primary level II, and a distinct tier-4 secondary selection such as regeneration.

| Pyramid tier | Normal range | Duration of each application |
| ---: | ---: | ---: |
| 1 | 20 blocks | 11 seconds |
| 2 | 30 blocks | 13 seconds |
| 3 | 40 blocks | 15 seconds |
| 4 | 50 blocks | 17 seconds |

A custom Paper beacon effect range is used when the server exposes one. Effects are refreshed every 80 server ticks—about four seconds at 20 TPS—so their normal durations overlap while the beacon remains valid.

?> The beacon's blocks and selected powers remain intact, and players receive the expected potion effects. Internally, however, these are plugin-applied effects rather than native beacon applications.

## Beam and blocker rules

Every tinted-glass block in the column is ignored for the replacement-effect calculation. The scan continues to the world's maximum height, so an opaque beam blocker placed before or after the tinted glass still stops replacement effects. Blocks accepted by the recognized vanilla obstruction rule—such as stained glass, panes, barrier, and bedrock—continue to pass the calculation.

Removing the final tinted-glass block makes the plugin stop tracking that beacon. If its column is otherwise clear, Minecraft resumes normal beacon handling and its native beam can return.

## Existing and changing beacons

| Situation | What the plugin does |
| --- | --- |
| Beacon service starts (startup while enabled, enable command, or reload while enabled) | Scans beacon tile entities in every currently loaded chunk |
| Existing beacon in an unloaded chunk | Discovers it when the chunk loads |
| New beacon or tinted glass is placed | Inspects the beacon or scans beacons below the new tinted glass |
| Beacon or relevant column block is broken or replaced | Removes or refreshes tracking after the block change |
| Piston or explosion changes relevant blocks | Schedules targeted rescans |
| Chunk unloads | Forgets tracked and active beacons in that chunk |
| Chunk loads again | Scans the chunk and rebuilds tracking |
| World unloads | Clears tracking for that world |

Tracked beacons are also refreshed every 80 ticks. There is no persistent beacon list and no repeating whole-world discovery scan. If another plugin changes blocks without producing the Bukkit events handled here, interacting with the beacon, reloading this plugin's configuration, reloading the chunk, or restarting the server may be needed before an untracked covered beacon is discovered.

## Restarts and compatibility fallback

On Paper 26.2, the plugin dynamically reads the server's beacon selection and light-dampening data. On other recognized server layouts it attempts the same approach, then falls back to the public Bukkit API if necessary.

The fallback corrects known material mismatches for tinted glass, bedrock, barrier, and slime blocks, but unusual technical block states can still differ from vanilla. If a covered beacon loses its secondary or level-II selection after a restart on a non-primary server version, temporarily uncover and reactivate the beacon before covering it again.

## Visual and plugin interoperability limits

On an unmodified Minecraft 26.2 client, tinted glass clears the complete native beam, including colored sections below the tinted glass. A server-only plugin cannot keep that lower section visible while ending the native beam at the glass. Doing so exactly would require client changes; SimpleNoBeamBeacon intentionally does not add a visual imitation.

Replacement effects do not fire Paper's `BeaconEffectEvent`. An `EntityPotionEffectEvent` sees SimpleNoBeamBeacon, rather than a beacon, as the effect cause. Plugins that depend on native beacon event attribution may therefore observe these effects differently.

The project declares no hooks into other plugins and exposes no supported public API, custom events, services, or extension points.

## Performance characteristics

Every tracked tinted-glass beacon has a repeating 80-tick refresh. A refresh scans its full vertical column and validates up to four pyramid layers. Every online player also has an 80-tick task that checks the active hidden beacons for world and range before applying effects. Chunk discovery inspects tile entities rather than every block.

Work therefore increases with the number of tracked hidden beacons and online players. The refresh interval is not configurable, and the repository defines no supported numeric capacity limit.
