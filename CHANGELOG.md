# Changelog

## 0.2.0-alpha

- Reject invalid config entries, unknown keys, fractional/overflowing numbers and uncovered tiers atomically.
- Ship one bundled default configuration; retain safe disabled behavior on invalid first startup.
- Add `/tnkloot validate` for missing installed items and empty live weapon pools.
- Reconcile existing advancements for online players after a successful reload.
- Normalize effective player ranks to configured tiers after config changes without deleting saved progress.
- Use an immutable weighted pool with unbiased sampling and totals above the 32-bit limit.
- Preserve extra tokens if an administrator overrides their normal stack size.
- Translate redemption, tier unlocks and token tooltips into English and Dutch.
- Add automated config, weighted selection and persistence tests, build tooling and project documentation.

## 0.1.0-alpha

- Initial standalone unidentified weapon, configurable Guild tiers, saved progression and optional FTB command/team bridge.