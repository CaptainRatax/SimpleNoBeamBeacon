# Installation and requirements

## Requirements

The current project version is `1.1.0`. Its release artifact is named `SimpleNoBeamBeacon-Paper-1.1.0.jar`.

| Server | Minecraft | Java | Status |
| --- | --- | --- | --- |
| Paper | 26.2 | 25 | Primary implementation and validation target |
| Paper, Spigot, or a compatible Bukkit-derived fork | 1.17.1 and newer | The version required by the server, with Java 16 as the plugin minimum | Additional compatibility baseline |

The JAR is compiled against the Spigot 1.17.1 API, declares `api-version: 1.17`, and uses Java 16 bytecode. Paper 26.2-specific checks are discovered at runtime, so Paper is not a hard dependency despite the artifact name.

On server versions whose internal beacon layout is not recognized, the plugin falls back to public Bukkit data and explicit obstruction exceptions. That fallback can differ from vanilla for unusual technical block states or may be unable to recover every covered secondary or level-II selection after a restart. Compatibility from 1.17.1 onward is therefore broader than the primary target, but is not a promise of identical behavior on every fork or future version.

!> Folia is not advertised as supported by the current plugin metadata. The current repository also contains no Sponge implementation or Sponge artifact.

### Dependencies

There are no required or optional plugin dependencies. Players do not need a client mod or resource pack for the preserved effects. Client software is relevant only if a different beam visual is desired; this plugin does not control client rendering.

## Install the plugin

1. Stop the Minecraft server.
2. Download the current installable JAR from [Modrinth](https://modrinth.com/plugin/simplenobeambeacon), [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/simplenobeambeacon), or [GitHub Releases](https://github.com/CaptainRatax/SimpleNoBeamBeacon/releases). You can also [build it from source](building.md).
3. Make sure you have `SimpleNoBeamBeacon-Paper-1.1.0.jar`, not the `-sources.jar` file.
4. Remove any older SimpleNoBeamBeacon JAR from the server. Do not keep version 1.0.0 and 1.1.0 installed together.
5. Copy the JAR into the server's `plugins/` directory.
6. Start the server. Use a full start or restart when installing or replacing the JAR.
7. Activate a beacon and select its powers before placing tinted glass in the same vertical column above it.

No documentation build, plugin dependency installation, or client installation is involved.

## Files created

On first startup, the plugin creates:

```text
plugins/
└── SimpleNoBeamBeacon/
    └── config.yml
```

No beacon database or other plugin-owned data file is created. Beacon tracking is rebuilt from loaded chunks whenever the service starts.

## Verify the installation

Check the server log for one of these messages:

```text
Loaded with beacon handling enabled.
Loaded with beacon handling disabled.
```

The disabled message means the plugin loaded successfully but `enabled: false` is set. An operator or the server console can also run `/snbb`; without a valid subcommand it reports the current state and usage.

Next, review [Configuration & Commands](administration.md) or continue to [How It Works](how-it-works.md).
