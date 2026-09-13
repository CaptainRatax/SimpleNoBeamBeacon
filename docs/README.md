# SimpleNoBeamBeacon

SimpleNoBeamBeacon 1.1.0 is a server-side Minecraft plugin that preserves beacon potion effects when tinted glass hides the native beacon beam. It is primarily implemented and validated for Paper 26.2, with an additional Bukkit-compatible baseline of Minecraft 1.17.1 and newer.

?> Tinted glass hides the beam through normal client behavior. The plugin does not remove beams with packets or change what blocks a player sees.

## What it changes

When the feature is enabled, the plugin handles only beacons with at least one tinted-glass block directly above them in the beacon column. For those beacons, it:

- validates the beacon pyramid and the complete column above it;
- treats tinted glass as transparent only for its replacement-effect calculation;
- keeps valid primary, upgraded primary, and secondary effects applying to eligible players;
- stops replacement effects if a real beam blocker is present; and
- returns the beacon to normal vanilla handling after the last tinted-glass block is removed.

The beacon block, pyramid, and selected powers are not changed. The replacement effects are applied by the plugin, rather than by Minecraft's native beacon effect process.

!> On an unmodified Minecraft 26.2 client, tinted glass hides the **entire** native beam, including the part below the glass. This plugin does not create a partial imitation with particles or display entities.

## Compatibility at a glance

| Item | Current repository information |
| --- | --- |
| Version | `1.1.0` |
| Primary target | Paper 26.2 / Minecraft 26.2 / Java 25 |
| Additional compatibility | Paper, Spigot, and compatible Bukkit-derived forks on Minecraft 1.17.1+ |
| Plugin bytecode | Java 16 |
| Runtime dependencies | None |
| Client requirements | No client mod or resource pack required |

Compatibility outside the primary target uses dynamically discovered server data when available and Bukkit fallbacks otherwise. Read [Installation & Requirements](installation.md) before choosing a server version.

## Get started

1. Check the [platform and Java requirements](installation.md#requirements).
2. Put the installable plugin JAR in the server's `plugins/` directory and restart the server.
3. Activate the beacon and choose its powers while it is uncovered.
4. Place tinted glass directly above the beacon to hide its native beam.

See [How It Works](how-it-works.md) for exact conditions, update behavior, and limitations. The plugin has one global setting and operator-only administration; see [Configuration & Commands](administration.md).

## Project

- [Source repository](https://github.com/CaptainRatax/SimpleNoBeamBeacon)
- [Releases](https://github.com/CaptainRatax/SimpleNoBeamBeacon/releases)
- [MIT license](https://github.com/CaptainRatax/SimpleNoBeamBeacon/blob/main/LICENSE)
