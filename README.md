# Dye Community Speedrun Mod v1.4.0

Official client-side Fabric mod for the Dye Community vanilla Dye Speedrun event.

## Target

- Minecraft Java Edition **26.2**
- Fabric Loader **0.19.3+**
- Fabric API **0.160.0+26.2**
- Java **25**
- Mod protocol version **1.0.0** (matches Dye Community Bot V8)

## What the mod does

- Links a Minecraft account to Discord using the one-time code from `/speedrun-link`.
- Retrieves the official attempt and server-generated seed from the V8 Speedrun API.
- Opens vanilla world creation with the official seed, Survival mode, structures on, bonus chest off and commands/cheats off.
- Verifies the actual loaded singleplayer seed before starting the official run.
- Runs an official HUD timer.
- **Pauses the timer whenever Minecraft itself is paused** (for example the ESC pause menu in singleplayer).
- Detects all 16 vanilla Dye items in the player's inventory.
- Reports run state to the API about every five seconds and immediately when Dye progress changes.
- Automatically stops and submits the time when all 16 Dyes are simultaneously present in the inventory.
- Reports seed, game mode and cheat state so the server can reject obvious rule violations.
- Restores an in-progress run after a game/mod restart by fetching the same run from the API. Offline time is not added to the active timer.

## Player flow

1. Install Fabric for Minecraft 26.2, Fabric API and this mod.
2. Start Minecraft and press **K** to open `Dye Community Speedrun`.
3. Set the public API URL supplied by the Dye Community (during local development it defaults to `http://localhost:3000`).
4. In Discord run `/speedrun-link`.
5. Enter the one-time code in the mod and click **Link Discord**.
6. In Discord run `/speedrun-start`.
7. In the mod click **Refresh**. It should show `Attempt #... ready` and `Seed received` without displaying the seed itself.
8. Click **Prepare Official World**.
9. The normal Minecraft world-creation screen opens with official settings prefilled. Click **Create New World**.
10. Once the actual world and seed are verified, the HUD timer starts automatically.
11. Collect all 16 Dyes. ESC may be used normally; while the singleplayer game is genuinely paused, the official timer pauses too.
12. At 16/16 the mod submits the run automatically to Discord/Bot V8.

The 16 required items are White, Light Gray, Gray, Black, Brown, Red, Orange, Yellow, Lime, Green, Cyan, Light Blue, Blue, Purple, Magenta and Pink Dye.

## Important fairness note

A client-side mod is not an unbeatable anti-cheat. It provides an official timer, seed binding, run identity and checks for obvious rule violations. A modified client can theoretically be tampered with. The server therefore remains the authority for accepting/invalidating attempts, and event staff can still request evidence for suspicious/top runs if desired.

## API compatibility

The mod speaks to the API already included in Dye Community Bot V8:

- `POST /api/v1/link/claim`
- `GET /api/v1/runs/pending`
- `POST /api/v1/runs/{runId}/start`
- `POST /api/v1/runs/{runId}/state`
- `POST /api/v1/runs/{runId}/finish`
- `POST /api/v1/runs/{runId}/abort`

The Discord bot token is **never** stored in or sent to this mod. The mod only stores its own API session token in `config/dye-speedrun.json`.

## Building locally

This project targets Java 25. Install a JDK 25 and Gradle 9.5.1, then from this directory run:

```text
gradle clean build
```

On Windows you can use `build-windows.bat` after Java and Gradle are on PATH. The release JAR will be in `build/libs/`.

Alternatively, push the project to GitHub and run the included **Build Dye Speedrun Mod** GitHub Actions workflow. It installs Java 25 and Gradle 9.5.1 and uploads the built JAR as an artifact.

## Before public distribution

Change the default API URL in `src/client/java/community/dye/speedrun/config/ModConfig.java` from localhost to the final Oracle HTTPS address, then build the release JAR. Users can also change the address in the in-game control screen.

## v1.5.0 release changes
- Official API is preconfigured as `https://dye-speedrun-api.dyecommunity.workers.dev`.
- Localhost configs are migrated automatically to the official cloud API.
- The normal control screen no longer exposes an API URL field.
- The mod version sent to the API is read from Fabric metadata instead of a hardcoded Java string.
