# QuestScript runtime support

This document describes the QuestScript expressions Quest Ledger 0.4.3 can evaluate in Minecraft 26.2.

## Evaluation lifecycle

Active quests are evaluated every five client ticks.

1. The current singleplayer world or multiplayer server scope is resolved.
2. Multiplayer clients periodically request the server's current vanilla statistics.
3. The completion expression is evaluated against the current player, synchronized statistics, saved baselines, and manual confirmation state.
4. If the expression is true, completion begins immediately.
5. A fixed internal 700 ms effect plays.
6. The quest is removed and its timestamp, ID, and title are appended to the scope's `completed-history.log`.

There is no configurable completion timer. Legacy timer fields are parseable but ignored.

A runtime value that cannot be evaluated is unknown, never true. Existing older files with deferred expressions remain loaded safely. When creating or replacing quests through the mod, runtime-support validation now rejects deferred expressions with `UNSUPPORTED_RUNTIME`, preventing permanently incomplete new quests.

Switching to another world or server unloads the previous scope and its completion effect. Returning reloads that scope's quests, baselines, manual state, and history.

## Completion modes

### Automatic

The expression contains only measurable values: statistics, inventory, location, player state, or supported logical combinations.

### Manual

```questscript
quest "금 공장 완성" {
  complete when {
    manual.checked == true
  }
}
```

Press **Complete** in Active Quests. The confirmation is persisted in `manual-state.properties` until the quest completes.

### Hybrid

```questscript
quest "금 공장 가동 확인" {
  complete when {
    manual.checked == true
    and inventory.count("minecraft:gold_ingot") >= 64
  }
}
```

Manual confirmation satisfies only `manual.checked`; all automatic conditions must also be true.

## Supported operators

- `and`
- `or`
- `not`
- `==`
- `!=`
- `>`
- `>=`
- `<`
- `<=`

Nested expressions and parentheses are supported.

## Supported player properties

| Property | Runtime value |
|---|---|
| `player.health` | Current health |
| `player.max_health` | Maximum health |
| `player.hunger` | Current food level |
| `player.experience_level` | Current experience level |
| `player.x`, `player.y`, `player.z` | Current coordinates |
| `player.dimension` | Current namespaced dimension ID |
| `player.on_ground` | Whether the player is on the ground |
| `player.is_sneaking` | Whether sneak is held |
| `player.is_sprinting` | Whether the player is sprinting |
| `manual.checked` | Persistent confirmation for this quest and scope |

## Supported inventory functions

| Function | Behavior |
|---|---|
| `inventory.count(id)` | Counts matching items in the inventory |
| `inventory.has(id)` | True when at least one matching item exists |
| `inventory.equipped(id)` | Checks equipment slots, including hands |
| `inventory.durability(id)` | Remaining durability of the first matching damageable stack |

Inventory functions are absolute. Use an exact namespaced ID. Tags are deferred and rejected for new saves.

## Supported statistic functions

| Function | Value returned |
|---|---|
| `stat.mined(block_id)` | Blocks mined since quest creation |
| `stat.used(item_id)` | Items used since quest creation |
| `stat.crafted(item_id)` | Items crafted since quest creation |
| `stat.killed(entity_id)` | Entities killed since quest creation |
| `stat.picked_up(item_id)` | Items picked up since quest creation |
| `stat.dropped(item_id)` | Items dropped since quest creation |

When a quest is saved, Quest Ledger snapshots every referenced statistic. Runtime evaluation subtracts the baseline from the latest vanilla value.

```questscript
quest "고대 잔해 네 개 더" {
  complete when {
    stat.mined("minecraft:ancient_debris") >= 4
  }
}
```

If the synchronized total is 100 when the quest is created, the expression begins at 0 and completes at 104.

### Multiplayer synchronization

While a multiplayer scope is active, Quest Ledger requests the server's current statistics every 100 client ticks. This keeps creation baselines and later deltas current even when the vanilla client has not otherwise refreshed the statistics screen.

The integration test starts a real Fabric server, sets stone mined to 40, verifies that the client stores 40 as the baseline, advances the server value to 43, and verifies completion of a `stat.mined("minecraft:stone") >= 3` quest after the next synchronization.

Baselines are stored in `runtime-state.properties` and survive restarts. If a server resets a statistic below its baseline, Quest Ledger safely rebases to the new value instead of producing a negative delta or false completion.

## Supported location and quest functions

```questscript
distance.to(100, 64, -30) <= 5
inside.box(0, 60, 0, 32, 90, 32) == true
inside.radius(100, 64, -30, 8) == true
quest.active("another_quest") == true
```

`quest.active(id_or_title)` checks the current scope's active store. The 0.4.3 parser fix allows the reserved `quest` namespace to be used correctly inside expressions.

## Deferred runtime expressions

The following syntax remains parseable for compatibility but is rejected when creating new quests:

- `world.time`
- `world.day`
- `world.is_day`
- `world.is_night`
- biome, weather, difficulty, and game-mode properties
- `advancement.done(...)`
- `quest.done(...)`
- item and block tags

Minecraft 26.2 introduced named World Clocks and Timelines. Future support will use explicit clock/timeline arguments rather than pretending that one global day-time value still exists.

## Save validation

QuestScript goes through two validation layers before the store is changed:

1. semantic validation checks names, argument counts, resource-ID shape, value types, comparison operators, animations, and duplicate quest IDs across the **entire merged store**;
2. runtime-support validation checks that each condition can be evaluated by the current Minecraft runtime.

A rejected append or replacement does not mutate the in-memory quest list or either transactional file.

## Per-world and per-server storage

Each scope receives its own directory:

```text
config/quest-ledger/worlds/<readable-name>-<scope-hash>/
  scope.properties
  quests.qs
  runtime-state.properties
  manual-state.properties
  completed-history.log
```

Singleplayer scopes use the normalized save path. Multiplayer scopes use the normalized server address. The readable prefix is cosmetic; the hash prevents collisions.

`quests.qs` and `runtime-state.properties` are committed in one recoverable transaction. Manual state is stored separately and is tied to persistent runtime UUIDs.

## Migration

The former client-wide quest and history files are migrated into the first opened scope and archived under `config/quest-ledger/legacy/`. A marker prevents copying them into later scopes.

Older versions did not record creation baselines. Migrated statistic quests therefore begin counting when first loaded by a baseline-aware version, which avoids accidental completion.

Legacy `hold` and `remove after` fields remain readable but have no runtime effect and are removed by user-facing formatting.
