# T&K Loot and Identification

Configurable unidentified weapons and Guild progression for **Minecraft 1.21.1 / NeoForge**.

[![Build](https://github.com/Mikaaah/TNK-Loot-Identification/actions/workflows/build.yml/badge.svg)](https://github.com/Mikaaah/TNK-Loot-Identification/actions/workflows/build.yml)
[Issues](https://github.com/Mikaaah/TNK-Loot-Identification/issues) · [Configuration](docs/configuration.md) · [FTB Quests setup](examples/ftb-quests-reward-guide.md)

**Download:** [tnkloot-0.3.0-alpha.jar](https://github.com/Mikaaah/TNK-Loot-Identification/raw/refs/heads/main/downloads/tnkloot-0.3.0-alpha.jar)

A weapon token records a Guild tier when awarded. Right-click it to identify a weighted weapon from that tier. The result is the original registered item, preserving its normal mod behavior and moveset integrations. This mod does not change item crafting or who can equip a weapon.

## Installation and quick start

Install the compiled `tnkloot` JAR on **both client and server**, with Java 21 and NeoForge **21.1.262 or newer** for Minecraft 1.21.1. Apotheosis, FTB Quests and FTB Teams are optional.

1. Start a world/server once to generate `config/tnkloot/tiers.json`.
2. Edit the tiers and weapon pools. See [the configuration guide](docs/configuration.md).
3. Run `/tnkloot reload`, then `/tnkloot validate` as an operator.
4. Run `/tnkloot unidentified @s` in game, then right-click the token.

The default config has eight tiers: Novice, Apprentice, Adventurer, Explorer, Veteran, Champion, Hero and Legendary. Vanilla advancement milestones unlock them automatically. Modpacks can replace the entire tier list, names, milestones and weighted weapon pools.

## Progression modes

| Mode | Behavior |
| --- | --- |
| `ADVANCEMENTS` | Unlock from configured advancements; checks existing progress on login and successful reload. Any listed advancement unlocks its tier. |
| `FTB_QUESTS` | Only commands grant progress. Use quest Command Rewards; no hard FTB dependency. |
| `MANUAL` | Only commands grant progress. Suitable for another quest or scripting system. |
| `OFF` | Every player has the highest configured tier. Identification remains available. |

Set `identification_enabled` to `false` to disable redemption. Set `share_ftb_teams` to `false` for individual progression. Optional team integration falls back to personal progress if the FTB Teams API is unavailable and logs a warning.

## Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/tnkloot tier` | Everyone | Show your effective tier |
| `/tnkloot tier list` | Everyone | List configured tier IDs and ranks |
| `/tnkloot tier unlock <players> <id or rank>` | Operator level 2 | Grant a tier, without lowering existing progress |
| `/tnkloot tier reset <players>` | Operator level 2 | Clear selected personal progress and their current shared team progress |
| `/tnkloot unidentified <players> [id or rank]` | Operator level 2 | Award a token using each recipient's tier or an explicit tier |
| `/tnkloot reload` | Operator level 2 | Validate and atomically reload settings |
| `/tnkloot validate` | Operator level 2 | Report missing registry items and empty live pools |

Use an explicit player name when running from the server console. Unidentified items awarded through ordinary `/give` or an Item Reward have no tier data and identify at tier 0. Use the mod's award command for tier-aware rewards.

## Behavior and boundaries

- Tokens keep their recorded **rank and display name** after player progression changes. Weapon definitions are evaluated when identified, so editing a pool also changes the potential result of existing tokens.
- Failed identification keeps the token. A full inventory drops newly awarded tokens/results safely when necessary.
- Tier grants are persistent per world, personal UUID and optionally current FTB team UUID. Leaving a team retains your own earned progress; a team-only rank does not become personal progress.
- Resets affect the current team's shared rank, but do not erase other members' personal progress. In advancement mode, qualifying advancements can restore ranks on login/reload. Use manual/quest mode for a persistent administrative rollback.
- Numeric tier ranks are save identities. Renaming a tier is safe; renumbering ranks changes the meaning of existing player data and tokens. Back up the world before changing rank assignments.
- Client tooltips use the tier name stored on awarded tokens. Server configuration determines redemption; clients do not need a copy of the server's weapon configuration.

Configured weapon IDs can now be replaced with tier-stamped tokens in entity, chest and other generated loot by enabling `replace_weapon_loot`. Each progression tier maps to the included Common → Ancient Apotheosis rarity ladder, and identified weapons receive Apotheosis affixes when that mod is installed. FTB Quests remains optional through command rewards; a custom FTB reward type and identification GUI are not implemented. FTB Teams multiplayer integration still needs validation against the pack's exact installed version. The token currently uses a vanilla placeholder model.

## Development

Use Java 21:

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Output: `build/libs/tnkloot-0.3.0-alpha.jar`. The `-sources.jar` is for development, not installation. Tests cover config validation/atomic reloads, weighted selection, progression persistence and optional integration settings. GitHub Actions runs the same build.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CHANGELOG.md](CHANGELOG.md). All rights reserved, matching the original project metadata.
