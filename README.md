# WebDisplays

WebDisplays lets you place special blocks in your Minecraft world that render
**live web pages** using MCEF (Minecraft Chromium Embedded Framework). Think
of it as putting a working browser window inside the game — you can browse
sites, watch embedded video, or build in-game "computer" setups using the
mod's screens, keyboards, and network blocks.

This repo contains the full source, built for multiple Minecraft versions
from a single Gradle multi-project build.

## 🖥️ How It Works

- **Screen blocks** render a Chromium browser instance directly onto a block
  face in the world, powered by MCEF running in a background process.
- **Keyboard & pointer blocks** let players standing near a screen actually
  type and click on the rendered page, just like a real browser.
- **Networking blocks** (linker, server, peripheral, etc.) let you wire
  screens together or drive them from redstone/computer setups.
- Each Minecraft version subproject (`v1_21`, `v1_21_1`, `v1_21_2`,
  `v1_21_4`) shares its core logic from `common/`, and only adds thin
  version-specific glue code where the Minecraft API changed between
  releases.

## Project Layout

```
webdisplays/
├── common/            # Shared code & resources used by every version
├── v1_21/              # Minecraft 1.21
├── v1_21_1/             # Minecraft 1.21.1
├── v1_21_2/             # Minecraft 1.21.2
├── v1_21_4/             # Minecraft 1.21.4
├── build.gradle         # Root build script (defines buildAll task)
└── settings.gradle       # Declares subprojects
```

Each version folder is its own Fabric Loom subproject. They all pull shared
Java sources and resources from `common/`, then add version-specific code on
top via `sourceSets`.

## Requirements

- **JDK 21** (Fabric Loom for MC 1.21+ requires Java 21)
- **Git** (to clone the repo)
- No IDE is strictly required — Gradle handles everything from the command
  line — but IntelliJ IDEA or VS Code with the Java extensions is recommended.

## Getting the Source

```bash
git clone https://github.com/userforfun06/webdisplayunofficial.git webdisplays
cd webdisplays
```

## Opening the Project

### IntelliJ IDEA
1. `File → Open`, select the root `webdisplays` folder.
2. Let Gradle sync — IntelliJ will detect all subprojects (`v1_21`,
   `v1_21_1`, `v1_21_2`, `v1_21_4`) automatically.

### VS Code
1. Open the root folder (or use `workspace.code-workspace` if present, which
   pre-configures each version as a separate workspace folder).
2. Install the "Extension Pack for Java" and "Gradle for Java" extensions if
   prompted.
3. Let the Java language server import each subproject.

## Building

Build a single version:

```bash
./gradlew :v1_21_4:build
```

Build every supported version at once:

```bash
./gradlew buildAll
```

Output JARs land in each subproject's `build/libs/` folder, e.g.
`v1_21_4/build/libs/webdisplays-<version>.jar`.

## Running / Testing In-Game

Fabric Loom provides run configurations for each subproject:

```bash
./gradlew :v1_21_4:runClient
./gradlew :v1_21_4:runServer
```

Swap `v1_21_4` for whichever version subproject you want to test
(`v1_21`, `v1_21_1`, `v1_21_2`).

The first run of each will download the matching Minecraft version and
decompile/remap it — this can take a few minutes.

## Making Changes

- **Shared logic** (used by all versions): edit files under `common/src/`.
- **Version-specific logic**: edit files under `<version>/src/main/java` or
  `<version>/src/client/java`.
- After editing shared code, just re-run `./gradlew :v1_21_4:build` (or
  `buildAll`) — Gradle picks up changes from `common/` automatically since
  it's included directly in each subproject's source sets.

## Cleaning

```bash
./gradlew clean
```

Removes all `build/` output directories across every subproject.

## Credits

Huge thanks to the original creators of this mod:

- **montoyo** — creator of the original WebDisplays mod.
- **CinemaMod Group** — maintained the upstream source this port builds on.

This project wouldn't exist without their work. Go check out their original
repo if you'd like to see where it all started!
