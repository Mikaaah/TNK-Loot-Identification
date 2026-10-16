#!/usr/bin/env python3
"""Static, offline checks for the default progression configuration."""
import json
from pathlib import Path
cfg = json.loads((Path(__file__).parent / "tiers.example.json").read_text(encoding="utf-8"))
assert cfg["progression_mode"] in {"ADVANCEMENTS", "FTB_QUESTS", "MANUAL", "OFF"}
assert cfg["tiers"][0]["rank"] == 0
assert len({t["rank"] for t in cfg["tiers"]}) == len(cfg["tiers"])
assert len({t["id"] for t in cfg["tiers"]}) == len(cfg["tiers"])
assert all(t["rank"] >= 0 for t in cfg["tiers"])
for w in cfg["weapons"]:
    assert w.get("category", "weapon") in {"weapon", "bow", "helmet", "chestplate", "leggings", "boots", "tool", "accessory"}
    assert w["min_tier"] <= w["max_tier"] and w["weight"] > 0
categories = {w.get("category", "weapon") for w in cfg["weapons"]}
for rank in [t["rank"] for t in cfg["tiers"]]:
    for category in categories:
        eligible = [w for w in cfg["weapons"] if w.get("category", "weapon") == category and w["min_tier"] <= rank <= w["max_tier"]]
        assert eligible, f"empty {category} pool for tier {rank}"
costs = cfg["identification_costs"]
assert costs["default"] >= 0 and all(int(tier) >= 0 and amount >= 0 for tier, amount in costs["per_tier"].items())
assert all(amount >= 0 for amount in costs["per_item"].values())
print(f"Passed: {len(cfg['tiers'])} tiers, {len(categories)} covered categories, valid weights and identification costs")
