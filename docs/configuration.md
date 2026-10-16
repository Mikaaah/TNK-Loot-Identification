# Configuration

Edit `config/tnkloot/tiers.json` on the server. A copy of the defaults is in [the example file](../examples/tiers.example.json). `/tnkloot reload` applies edits without restarting. Invalid settings reject the **whole reload** and retain the last good in-memory settings; the edited file is left untouched. On a first launch with invalid config and no previous good settings, identification stays disabled until corrected.

## Global keys

| Key | Default | Meaning |
| --- | --- | --- |
| `progression_mode` | `ADVANCEMENTS` | `ADVANCEMENTS`, `FTB_QUESTS`, `MANUAL` or `OFF` |
| `share_ftb_teams` | `true` | Read and grant the current optional FTB team's shared rank |
| `identification_enabled` | `true` | Allow right-click redemption |
| `replace_weapon_loot` | `false` | Replace configured weapon stacks in generated loot with unidentified tokens |
| `tiers` | Required array | Tier definitions; rank 0 required |
| `weapons` | Required array | Weighted weapon definitions |

Unknown keys are rejected to catch misspellings. Booleans must be JSON booleans; ranks/weights must be whole numbers within Java's integer range.

## Tiers

```json
{"rank": 3, "id": "explorer", "name": "Explorer", "advancements": ["minecraft:story/mine_diamond"], "apotheosis_rarity": "apotheosis:epic"}
```

Ranks must be unique, nonnegative and below 2147483647. They can have gaps. IDs must be unique, using letters, digits and underscores; they normalize to lowercase. `name` defaults to the ID. `advancements` is optional; **any** listed completed advancement grants the tier in advancement mode. An empty list means no automatic grant. Unlocking a higher tier does not require earlier milestones. `apotheosis_rarity` is optional and defaults to the Common, Uncommon, Rare, Epic or Mythic ladder according to rank. When Apotheosis is installed, it selects the rarity used to generate native Apotheosis affixes when a token is identified. Map multiple Guild tiers to one rarity, or use your own datapack rarity ID.

## Weapon pools

```json
{"item": "examplemod:steel_sword", "min_tier": 2, "max_tier": 4, "weight": 5}
```

`min_tier` is required. `max_tier` is inclusive and defaults to 2147483647. `weight` defaults to 1 and must be positive. Weight is relative to other available eligible entries: weights 5 and 1 produce probabilities 5/6 and 1/6. Repeating the same item in overlapping entries adds its weights.

The item ID can belong to any installed mod; no hard integration is needed. Missing optional mod IDs and `minecraft:air` are skipped at runtime. At least one configured weapon must cover every configured rank while identification is enabled. Run `/tnkloot validate` after loading to ensure those IDs also exist in this installation. An empty live pool refuses redemption and preserves the token.

A token's recorded rank is matched directly to these inclusive ranges, even if that rank is later removed from the config. Keep old ranges covered if you want existing tokens to remain usable. Loot results are fresh original item stacks, preserving the original mod item and its moveset integrations.

## Loot replacement and Apotheosis

`replace_weapon_loot` is off by default. When enabled, the global loot modifier replaces every generated stack whose item ID is in `weapons` with one tier-stamped token per item. This covers entity and chest loot tables, including tables added by other mods. Loot generated with a player context uses that player's effective tier; context-free loot (usually chests) uses rank 0. Add the weapons you want covered to the weapon list. Disable this option to keep loot tables unchanged.

Apotheosis is detected at runtime and remains optional. For each tier, `apotheosis_rarity` must match a rarity registered by Apotheosis or a datapack. Identification applies Apotheosis' normal rarity rules and affixes to the selected weapon. If Apotheosis is absent, the token still identifies and returns the unmodified weapon. If a configured rarity is unavailable, the mod logs one warning and safely returns the weapon without affixes.
