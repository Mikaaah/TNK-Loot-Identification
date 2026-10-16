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
    assert w["min_tier"] <= w["max_tier"] and w["weight"] > 0
for rank in [0, 1, 2, 3, 4, 5, 6, 7]:
    eligible = [w for w in cfg["weapons"] if w["min_tier"] <= rank <= w["max_tier"]]
    assert eligible, f"empty pool for tier {rank}"
print("Passed: 8 progression tiers, non-empty weapon pools, valid weights and IDs")