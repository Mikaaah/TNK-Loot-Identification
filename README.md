# T&K Loot and Identification

Configurable unidentified weapons, armor and tools with Guild progression for **Minecraft 1.21.1 / NeoForge**.

[![Build](https://github.com/Mikaaah/TNK-Loot-Identification/actions/workflows/build.yml/badge.svg)](https://github.com/Mikaaah/TNK-Loot-Identification/actions/workflows/build.yml)
[Issues](https://github.com/Mikaaah/TNK-Loot-Identification/issues) · [Configuration](docs/configuration.md) · [FTB Quests setup](examples/ftb-quests-reward-guide.md)

**Builds:** [Download the latest successful build](https://github.com/Mikaaah/TNK-Loot-Identification/actions/workflows/build.yml) → open a successful run → Artifacts → TNK-Loot-0.6.1-alpha. Install the plain JAR; the sources JAR is for developers.

Each category token records its Guild tier and a stable roll seed when awarded. Bring tokens to a Guild Identifier. Its one-row inventory accepts up to seven items, shows the total cost, and reveals each result with a short item roll and note-block sounds after confirmation. Results retain the original registered item's behavior and moveset integrations. This mod does not change item crafting or who can equip an item.

Find the Identifier block and all eight unidentified item types in the **T&K Loot** Creative inventory tab.

## Installation and quick start

Install the compiled `tnkloot` JAR on **both client and server**, with Java 21 and NeoForge **21.1.262 or newer** for Minecraft 1.21.1. Apotheosis is required, along with Placebo, Patchouli, Apothic Attributes, Apothic Spawners and Apothic Enchanting. FTB Quests and FTB Teams remain optional.

1. Start a world/server once to generate `config/tnkloot/tiers.json`.
2. Edit the tiers and weapon pools. See [the configuration guide](docs/configuration.md).
3. Run `/tnkloot reload`, then `/tnkloot validate` as an operator.
4. Run `/tnkloot unidentified @s` in game, place an Identifier (`/give @s tnkloot:identifier`), open its inventory, insert the token and confirm the shown cost. Add a category, for example `/tnkloot unidentified @s explorer helmet`, to award a different token type.

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
| `/tnkloot unidentified <players> [tier] [category]` | Operator level 2 | Award a category token; categories: weapon, bow, helmet, chestplate, leggings, boots, tool, accessory |
| `/tnkloot reload` | Operator level 2 | Validate and atomically reload settings |
| `/tnkloot validate` | Operator level 2 | Report missing registry items and empty live pools |

Use an explicit player name when running from the server console. Unidentified items awarded through ordinary `/give` or an Item Reward have no tier data and identify at tier 0. Use the mod's award command for tier-aware rewards. `/tnkloot unidentified @s explorer helmet` gives a tier-specific helmet token.

## Behavior and boundaries

- Tokens keep their recorded **rank and display name** after player progression changes. Weapon definitions are evaluated when identified, so editing a pool also changes the potential result of existing tokens.
- Failed identification keeps the token. A full inventory drops newly awarded tokens/results safely when necessary.
- Tier grants are persistent per world, personal UUID and optionally current FTB team UUID. Leaving a team retains your own earned progress; a team-only rank does not become personal progress.
- Resets affect the current team's shared rank, but do not erase other members' personal progress. In advancement mode, qualifying advancements can restore ranks on login/reload. Use manual/quest mode for a persistent administrative rollback.
- Numeric tier ranks are save identities. Renaming a tier is safe; renumbering ranks changes the meaning of existing player data and tokens. Back up the world before changing rank assignments.
- Client tooltips use the tier name stored on awarded tokens. Server configuration determines redemption; clients do not need a copy of the server's weapon configuration.

Configured equipment IDs are replaced by matching category tokens in mob drops, boss rewards, structure chests and modded loot tables. The default config covers vanilla swords, bows, armor and tools, with emerald costs from 1 to 25 per tier. Prices can be customized by tier and item. The Identifier block spawns an attendant behind the counter, facing it, with arrival/departure particles. The attendant is protected against survival and creative attacks. Right-click the block or villager to open the seven-slot counter. Handheld identification has been removed. Apotheosis rarity and real affixes are applied during identification. Seven custom rarities have 23 matching stat, effect and ability affix definitions; a bad rarity/category/pool refuses the whole batch before payment. Pools and costs are configured in JSON and reloaded with `/tnkloot reload`; Operators can use /tnkloot editor to manage tier rarity mappings and equipment pools in game; tier names, IDs and advancement unlocks remain editable in JSON. FTB Teams integration still needs validation against the pack's exact installed version. Token icons currently use vanilla placeholder art until custom textures are provided.

## Development

Use Java 21:

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Output: `build/libs/tnkloot-0.6.1-alpha.jar`. The `-sources.jar` is for development, not installation. Tests cover config validation/atomic reloads, category pools, identification prices, weighted selection and progression persistence. GitHub Actions runs the build, unit tests and a dedicated-server integration test that generates every configured tier/category, verifies seven-item payment and safe close, and checks the NPC lifecycle.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [CHANGELOG.md](CHANGELOG.md). All rights reserved, matching the original project metadata.

See [Apotheosis presets](docs/apotheosis-presets.md) for the affix ladder and datapack customization. Existing `tiers.json` files are preserved; new worlds receive the improved material pools. Apply the [example](examples/tiers.example.json) manually if you want those pool changes in an existing pack.
