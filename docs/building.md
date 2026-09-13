# Building from source

SimpleNoBeamBeacon uses a Gradle Kotlin DSL build and includes the Gradle 9.6.1 wrapper. Maven is not used.

## Prerequisites

- JDK 25 available to Gradle;
- a checkout of the repository; and
- network access on the first build so the wrapper and declared dependencies can be downloaded.

JDK 25 is required even though the installable plugin uses Java 16 bytecode. The build includes a separate source-compatibility check against the Paper 26.2 API that compiles with Java 25.

## Build

From the repository root on Linux or macOS:

```bash
bash ./gradlew clean build
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

The `build` task runs the JUnit test suite and the Paper 26.2 compile check.

## Output

Artifacts are written to `bukkit/build/libs/`:

```text
bukkit/build/libs/
├── SimpleNoBeamBeacon-Paper-1.1.0.jar
└── SimpleNoBeamBeacon-Paper-1.1.0-sources.jar
```

Install `SimpleNoBeamBeacon-Paper-1.1.0.jar` on the server. The sources JAR is for source browsing and is not a server plugin artifact.

## Build dependencies

The main module compiles against the Spigot 1.17.1 API as a `compileOnly` dependency. Tests use JUnit 5, and the compatibility check resolves the Paper 26.2 API. None of these are bundled runtime plugin dependencies.

The repository contains one `bukkit` module and does not expose a supported public API, service, custom event, or extension point for other plugins.

## Documentation publishing

The files in `docs/` are rendered directly in the browser by Docsify 4. They require no npm installation or documentation build. GitHub Pages can publish that directory directly from the `main` branch.
