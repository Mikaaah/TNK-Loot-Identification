# Configuration

Edit `config/tnkloot/tiers.json` on the server. A copy of the defaults is in [the example file](../examples/tiers.example.json). `/tnkloot reload` applies edits without restarting. Invalid settings reject the **whole reload** and retain the last good in-memory settings; the edited file is left untouched. On a first launch with invalid config and no previous good settings, identification stays disabled until corrected.

## Global keys

| Key | Default | Meaning |
| --- | --- | --- |
| `progression_mode` | `ADVANCEMENTS` | `ADVANCEMENTS`, `FTB_QUESTS`, `MANUAL` or `OFF` |
| `share_ftb_teams` | `true` | Read and grant the current optional FTB team's shared rank |
| `identification_enabled` | `true` | Allow item redemption |
| `handheld_identification` | `true` | Allow right-click redemption |
| `station_identification` | `true` | Allow redemption at the Identifier block |
| `replace_weapon_loot` | `true` | Replace configured item stacks in generated loot with category-specific tokens |
| `identification_costs` | Object | Configure currency and prices |
| `tiers` | Required array | Tier definitions; rank 0 required |
| `weapons` | Required array | Weighted equipment entries (name retained for config compatibility) |

Unknown keys are rejected to catch misspellings. Booleans must be JSON booleans; ranks/weights must be whole numbers within Java's integer range.

## Tiers

```json
{"rank": 3, "id": "explorer", "name": "Explorer", "advancements": ["minecraft:story/mine_diamond"], "apotheosis_rarity": "apotheosis:epic"}
```

Ranks must be unique, nonnegative and below 2147483647. They can have gaps. IDs must be unique, using letters, digits and underscores; they normalize to lowercase. `name` defaults to the ID. `advancements` is optional; **any** listed completed advancement grants the tier in advancement mode. An empty list means no automatic grant. Unlocking a higher tier does not require earlier milestones. `apotheosis_rarity` is optional and defaults to this mod's Common → Uncommon → Rare → Epic → Legendary → Mythic → Ancient ladder according to rank. When Apotheosis is installed, it selects the rarity used to generate native Apotheosis affixes when a token is identified. The mod supplies these seven rarities; their natural world-loot weights are zero, so they are awarded through this progression system. Map multiple Guild tiers to one rarity, or use your own datapack rarity ID.

## Equipment pools

```json
{"item": "examplemod:steel_sword", "category": "weapon", "min_tier": 2, "max_tier": 4, "weight": 5}
```

`min_tier` is required. `max_tier` is inclusive and defaults to 2147483647. `weight` defaults to 1 and must be positive. Weight is relative to other available eligible entries: weights 5 and 1 produce probabilities 5/6 and 1/6. Repeating the same item in overlapping entries adds its weights.

Categories are `weapon`, `bow`, `helmet`, `chestplate`, `leggings`, `boots`, `tool` and `accessory`. `category` defaults to `weapon` for older config files. A configured category needs at least one eligible item at every tier; unconfigured categories do not produce tokens. The item ID can belong to any installed mod. Missing mod items and `minecraft:air` are skipped at runtime. Run `/tnkloot validate` after loading to ensure those IDs also exist in this installation. An empty live pool refuses redemption and preserves the token.

A token's recorded rank is matched directly to these inclusive ranges, even if that rank is later removed from the config. Keep old ranges covered if you want existing tokens to remain usable. Loot results are fresh original item stacks, preserving the original mod item and its moveset integrations.

## Loot replacement and Apotheosis

`replace_weapon_loot` is on by default. The global loot modifier replaces every generated stack whose exact item ID is configured with one category-specific, tier-stamped token per item. For example, a configured `minecraft:wooden_sword` stack becomes `tnkloot:unidentified_weapon`; an iron helmet becomes `tnkloot:unidentified_helmet`. This applies to vanilla and modded entity, chest, boss and other loot tables without replacing unrelated drops. Loot generated with a player context snapshots that player's effective tier; context-free loot (usually structure chests) uses rank 0. Removing an item from the registry stops future conversions. Disable the option to preserve the original loot.

## Identification costs

```json
"identification_costs": {
  "enabled": true,
  "item": "minecraft:emerald",
  "default": 1,
  "per_tier": {"0": 1, "1": 2, "2": 3, "3": 5, "4": 8, "5": 12, "6": 18, "7": 25},
  "per_item": {"minecraft:netherite_sword": 32}
}
```

Costs are charged from the player's inventory and offhand. Per-item prices override per-tier prices, which override `default`. Set `enabled` to `false` for free identification. A token remains intact when the pool is empty or the player cannot pay. Each issued token stores its tier and a stable roll seed so retrying after a failed payment cannot reroll the selected base item.

## Identifier block

Craft or grant `tnkloot:identifier`. Place it to spawn an invulnerable, no-AI Guild Identifier beside the counter. Use an unidentified token on the block to identify it using the same cost rules as handheld use. Breaking the block removes its attendant. The block is usable on a dedicated server; it does not require a client-side GUI screen.

Apotheosis is a required runtime dependency. Install Apotheosis 1.21.1-8.9.0 or newer together with its required mods: Placebo, Apothic Attributes, Apothic Spawners, Apothic Enchanting and Patchouli. For each tier, `apotheosis_rarity` must match a rarity registered by Apotheosis or a datapack. Identification applies Apotheosis' normal rarity rules and affixes to eligible selected items. If a configured rarity is unavailable, the mod logs one warning and safely returns the base item without affixes.
