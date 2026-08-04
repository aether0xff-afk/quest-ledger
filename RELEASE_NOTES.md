# Quest Ledger v0.4.3

Quest Ledger v0.4.3 is a full functionality audit and reliability release. It keeps the v0.4.2 pixel interface and storage format while fixing several problems that were invisible to the earlier visual and compile-only tests.

## Fixed during the audit

- Validates the complete merged quest store before appending, so a duplicate quest ID can no longer be written and break loading on the next launch.
- Rejects newly created QuestScript conditions that the current runtime cannot evaluate. Deferred syntax remains parseable for compatibility, but users receive an `UNSUPPORTED_RUNTIME` error instead of a quest that can never finish.
- Fixes parsing of the documented `quest.active(...)` function.
- Changes the default ledger key from `Q` to `K`, avoiding Minecraft's default Drop Item binding. Existing users can still choose another key in Controls.
- Requests live vanilla statistics from multiplayer servers every five seconds while a scope is active. Creation-relative mining, crafting, use, pickup, drop, and kill quests therefore use synchronized server values rather than stale client data.

## Full verification

The Java 25/Fabric build passes with:

- Minecraft/Fabric main, client, and test compilation;
- 20,297 save-facing QuestScript checks covering builder-equivalent output, supported and deferred runtime expressions, duplicate IDs, format round trips, and deterministic malformed-input fuzzing;
- the existing QuestScript parser and formatter suite;
- 19 transaction scenarios covering every recoverable interruption point for existing stores and first saves;
- responsive UI geometry and text fitting across nine logical resolutions.

A real Minecraft 26.2 Fabric server and client are also exercised end to end:

1. The first client launch creates automatic, manual, statistic, and `quest.active` quests.
2. Duplicate IDs and unsupported runtime conditions are rejected without changing the active store.
3. Manual, inventory, and `quest.active` quests complete and are written to history.
4. The server sets the stone-mined statistic to 40. The client synchronizes that value and saves it as the quest baseline.
5. The server advances the statistic to 43. After the next synchronization, the `stat.mined(...) >= 3` quest completes and is written to history.
6. The client exits completely and reconnects. The remaining quest and runtime store are restored from disk.
7. The second launch clears the store and confirms that it remains empty.

The standard, compact, and micro real-client visual profiles also remain green after the functionality changes.

## Runtime support policy

The following deferred expressions remain readable in older files but cannot be added as new quests until implemented:

- world clock, weather, difficulty, biome, and game-mode properties;
- `advancement.done(...)` and `quest.done(...)`;
- item and block tags.

Exact item/block/entity IDs, player state, inventory, equipment, durability, creation-relative statistics, location functions, `quest.active(...)`, manual confirmation, and logical combinations are supported.

## Compatibility

The quest format and per-world/per-server storage remain compatible with v0.4.0–v0.4.2. Existing quests, runtime statistic baselines, manual confirmations, and completion history are retained. Existing deferred expressions are loaded safely as unknown rather than being deleted.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3+
- Fabric API 0.156.0+26.2
- Java 25+

## Installation

Remove older Quest Ledger JARs and place `quest-ledger-0.4.3.jar` in the instance's `mods` directory. Keep a normal world backup before installing any mod update.
