# Agentic Babymode

Server-side tuning mod for Minecraft 1.19.4 (Fabric) aimed at agentic / "baby mode"
gameplay: weaker mobs, faster and easier player actions, plus persistent
nutrition and fatigue systems.

> **For Open-to-LAN hosting, install Agentic Babymode on the hosting Minecraft
> client. Connecting players/bots do not need the mod.** The mod loads inside
> the host client process but its gameplay logic only runs on the logical
> server side, so it works on a LAN integrated server and on a dedicated
> Fabric server with the same jar.

## Features

- **Combat tuning**
  - Mobs deal `mobs.damageMultiplier` damage (default 0.35×)
  - Mobs take `mobs.damageTakenMultiplier` from player melee (default 2×)
  - Mobs attack `mobs.attackCooldownMultiplier` slower (default 3× = 60 ticks between hits)
  - Mobs move at `mobs.movementSpeedMultiplier` (default 0.35×)
  - PvP can be disabled (`player.pvp`)
- **Player comfort**
  - `player.movementSpeedMultiplier` (default 1.5×)
  - `player.miningSpeedMultiplier` (default 5×, also affected by fatigue)
  - `environment.drowningTimeMultiplier` (default 10× longer underwater)
  - Natural regeneration can be forced off (`player.naturalRegeneration`)
- **Fatigue** (per player, persisted in the world)
  - Rises from sprinting, breaking blocks and taking damage
  - Recovers while idle and especially while sleeping
  - At high fatigue: slowness, mining fatigue, movement penalty, cannot sprint
- **Nutrition** (per player, persisted in the world)
  - Three categories: grain, protein, produce (0–100 each)
  - Eating food from the catalog (bread, steak, apples, …) raises them
  - They decay every in-game day
  - Well-fed (avg ≥ 75) grants regeneration; malnourished (avg ≤ 20) grants weakness

## Commands

```
/babymode version
/babymode reload          # reload config/agentic-babymode.json
/babymode status          # human-readable player state
/babymode status json     # one compact JSON line, easy for bots to parse
```

`/babymode status json` output example:

```json
{"version":"0.1.0","fatigue":73.0,"nutrition":{"grain":89.0,"protein":41.0,"produce":67.0},"health":17.0,"hunger":14,"air":300}
```

## Config

`config/agentic-babymode.json` is created with defaults on first launch.
Every value is optional; missing values fall back to defaults.

```json
{
  "player": {
    "movementSpeedMultiplier": 1.5,
    "miningSpeedMultiplier": 5.0,
    "naturalRegeneration": false,
    "pvp": false
  },
  "environment": {
    "drowningTimeMultiplier": 10.0
  },
  "mobs": {
    "movementSpeedMultiplier": 0.35,
    "attackCooldownMultiplier": 3.0,
    "damageMultiplier": 0.35,
    "damageTakenMultiplier": 2.0
  },
  "fatigue": {
    "enabled": true,
    "sprintCostPerSecond": 1.5,
    "mineCostPerBlock": 0.75,
    "damageCostPerPoint": 0.25,
    "idleRecoveryPerSecond": 1.0,
    "sleepRecoveryPerSecond": 3.0,
    "movementPenaltyPercent": 50.0
  },
  "nutrition": {
    "enabled": true,
    "decayPerDay": 10.0,
    "wellFedRegenThreshold": 75.0,
    "malnourishedThreshold": 20.0
  }
}
```

## Installation (LAN host)

1. Install Fabric API (any 1.19.4 release, e.g. `0.87.2+1.19.4`) as a normal mod.
2. Drop `agentic-babymode-0.1.0.jar` into the instance's `mods` folder.
3. Launch, open a world, **Open to LAN**.
4. Join with Mineflayer or vanilla clients — they do **not** need the mod.

## Development

Requirements: JDK 17, Git, internet (Gradle fetches Loom/Yarn/Fabric automatically).

```
./gradlew.bat build        # compile + test + produce build/libs/agentic-babymode-0.1.0.jar
./gradlew.bat runServer    # headless smoke test (eula.txt in run/)
./gradlew.bat runClient    # full client (needs a display)
```

Pinned toolchain: Gradle 8.5 (wrapper), Fabric Loom 1.2.4, Yarn `1.19.4+build.2`,
Fabric Loader 0.19.3, Fabric API 0.87.2+1.19.4. Newer Loom (1.17+) requires
JVM 21, which is why the 1.2.4 pin matters for a Java 17 environment.

## Architecture

- `system/` — pure Java logic (nutrition, fatigue, combat math, food catalog), unit-tested with JUnit 5
- `config/` — Gson config with defaults
- `state/` — per-UUID player state persisted via Minecraft `PersistentState` (world NBT, no SQLite/JSON files)
- `server/` — Fabric API event wiring (server lifecycle, player join, ticks, block break, entity load)
- `mixin/` — only where Fabric has no hook: melee damage scaling (`@Redirect` on the `damage` call sites), drowning air, mining speed, eating
- `command/` — Brigadier `/babymode` command tree

## License

MIT — see [LICENSE](LICENSE).
