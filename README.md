# Agentic Babymode

Server-side tuning mod for Minecraft 1.19.4 (Fabric) aimed at agentic / "baby mode"
gameplay: weaker mobs, faster and easier player actions, plus persistent
sleepiness and nutrition systems that force strategic food planning.

> **For Open-to-LAN hosting, install Agentic Babymode on the hosting Minecraft
> client. Connecting players/bots do not need the mod.** The mod loads inside
> the host client process but its gameplay logic only runs on the logical
> server side, so it works on a LAN integrated server and on a dedicated
> Fabric server with the same jar.

## Features

- **Combat tuning**
  - Mobs deal `mobs.damageMultiplier` damage (default 1/15 ≈ 0.067×)
  - Mobs take `mobs.damageTakenMultiplier` from player melee (default 2×)
  - Mobs attack `mobs.attackCooldownMultiplier` slower (default 3× = 60 ticks between hits)
  - Mobs move at `mobs.movementSpeedMultiplier` (default 1/15 ≈ 0.067×)
  - PvP can be disabled (`player.pvp`)
- **Disabled mobs** (fully removed from the world on spawn/chunk-load, and can never
  damage the player as a failsafe). Defaults remove **phantoms, creepers and pillagers**
  (`mobs.disablePhantoms` / `disableCreepers` / `disablePillagers`). Set any to `false` to re-enable.
- **Player comfort**
  - `player.movementSpeedMultiplier` (default 1.5×), `player.miningSpeedMultiplier` (default 5×)
  - `environment.drowningTimeMultiplier` (default 10× longer underwater)
  - Natural regeneration forced off (`player.naturalRegeneration`)
  - Items within `player.pickupRange` blocks of a player get pulled into their
    inventory (default 6.0; vanilla pickup rules still apply — no new mixin)
  - **"Kicked for spamming" disabled** — vanilla kicks players who send chat messages or
    run commands faster than ~1/sec sustained; too aggressive for agents, so the spam
    check is cancelled entirely (server-side mixin)
- **Sleepiness** (per player, persisted in the world)
  - New players start at 0 sleepiness
  - Builds up purely over in-game time: 0 → 100 over `sleepiness.fullAfterDays` (default 30 days)
  - **The only reset is right-clicking a bed** — works even during the day
  - At 50+: Slowness / Mining Fatigue; at 100: can't sprint; movement & mining slow down
- **Nutrition** (per player, persisted in the world: grain / protein / produce, 0–100 each)
  - New players start at 100/100/100 nutrition
  - Eaten food from the catalog raises categories (bread→grain, meat→protein, fruit→produce)
  - Each category decays to 0 over `nutrition.daysToEmpty` (default 90 days)
  - **Dying resets nutrition to 100/100/100 and sleepiness to 0** (soft reset)
  - **Grain < 40 / < 15 → Slowness I / II**
  - **Protein < 40 / < 15 → Weakness I / II**
  - **Produce < 40 / < 15 → Mining Fatigue I / II**
  - **avg < 10 → STARVING: −4 max HP, Hunger drain, no sprint** — if you don't eat, the
    food bar hits 0 and vanilla starvation damage kills you (a real deadline)
  - **avg ≥ 75 → well-fed buffs (Regeneration + Strength + Haste)** — only when
    `nutrition.enableWellFedBuffs` is `true` (default `false`, off, so agents aren't
    confused by buffs)
- **Hunger pacing** (vanilla food bar)
  - Drains to empty in `environment.hungerPassiveDaysToEmpty` (default 30 days) while idle
  - Action exhaustion scaled by `environment.hungerActivityExhaustionMultiplier` (default 1.0 = vanilla)
  - Starving additionally drains the food bar at `environment.starvingFoodDrainPerSecond`
- **Chat notifications** (normal chat, so bots/Mineflayer capture them like any message)
  - Debuffs applied/removed with reasons ("Slowness II (grain 8/100)")
  - Starving entered/exited, well-fed entered/ended
  - Bed interaction confirmation
  - Set `chat.notificationsEnabled` to `false` to suppress all of the above. `/babymode`
    command replies are always shown regardless of this setting.

## Commands

```
/babymode version
/babymode help          # full current rule set from config — call once when a bot goes online
/babymode reload        # reload config/agentic-babymode.json (op)
/babymode reset <0-100> # set all players' grain/protein/produce to N and sleepiness to 0 (op)
/babymode status        # human-readable state
/babymode status json   # one compact JSON line, easy for bots to parse
```

`/babymode status json` output example:

```json
{"version":"0.2.0","sleepiness":73.0,"fatigue":73.0,"nutrition":{"grain":89.0,"protein":41.0,"produce":67.0},"health":17.0,"maxHealth":20.0,"hunger":14,"air":300,"effects":[{"id":"minecraft:slowness","amplifier":1,"duration":190}]}
```

The `fatigue` field is an alias for `sleepiness` (older integrations keep working).

## Config

`config/agentic-babymode.json` is created with defaults on first launch.
Every value is optional; missing values fall back to defaults.

```json
{
  "player": {
    "movementSpeedMultiplier": 1.5,
    "miningSpeedMultiplier": 5.0,
    "naturalRegeneration": false,
    "pvp": false,
    "pickupRange": 6.0
  },
  "environment": {
    "drowningTimeMultiplier": 10.0,
    "hungerPassiveDaysToEmpty": 30.0,
    "hungerActivityExhaustionMultiplier": 1.0,
    "starvingFoodDrainPerSecond": 0.02
  },
  "mobs": {
    "movementSpeedMultiplier": 0.0667,
    "attackCooldownMultiplier": 3.0,
    "damageMultiplier": 0.0667,
    "damageTakenMultiplier": 2.0,
    "disablePhantoms": true,
    "disableCreepers": true,
    "disablePillagers": true
  },
  "sleepiness": {
    "enabled": true,
    "fullAfterDays": 30.0,
    "penaltyPercent": 50.0
  },
  "nutrition": {
    "enabled": true,
    "daysToEmpty": 90.0,
    "enableWellFedBuffs": false,
    "deficitThresholdI": 40.0,
    "deficitThresholdII": 15.0,
    "starvingThreshold": 10.0,
    "wellFedThreshold": 75.0,
    "preventSprintWhenStarving": true
  },
  "chat": {
    "notificationsEnabled": true
  }
}
```

## Installation (LAN host)

1. Install Fabric API (any 1.19.4 release, e.g. `0.87.2+1.19.4`) as a normal mod.
2. Drop `agentic-babymode-0.2.0.jar` into the instance's `mods` folder.
3. Launch, open a world, **Open to LAN**.
4. Join with Mineflayer or vanilla clients — they do **not** need the mod.

## Development

Requirements: JDK 17, Git, internet (Gradle fetches Loom/Yarn/Fabric automatically).

```
./gradlew.bat build        # compile + test + produce build/libs/agentic-babymode-0.2.0.jar
./gradlew.bat runServer    # headless smoke test (eula.txt in run/)
./gradlew.bat runClient    # full client (needs a display)
```

Pinned toolchain: Gradle 8.5 (wrapper), Fabric Loom 1.2.4, Yarn `1.19.4+build.2`,
Fabric Loader 0.19.3, Fabric API 0.87.2+1.19.4. Newer Loom (1.17+) requires
JVM 21, which is why the 1.2.4 pin matters for a Java 17 environment.

## Architecture

- `system/` — pure Java logic (nutrition, sleepiness, combat math, food catalog), unit-tested with JUnit 5
- `config/` — Gson config with defaults
- `state/` — per-UUID player state persisted via Minecraft `PersistentState` (world NBT, no SQLite/JSON files)
- `server/` — Fabric API event wiring (server lifecycle, player join, ticks, entity load, bed interaction, chat reporting)
- `mixin/` — only where Fabric has no hook: melee damage scaling (`@Redirect` on the `damage` call sites), drowning air, mining speed, eating, exhaustion scaling
- `command/` — Brigadier `/babymode` command tree

## License

MIT — see [LICENSE](LICENSE).
