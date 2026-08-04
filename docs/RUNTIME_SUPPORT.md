# QuestScript runtime support

This document describes which QuestScript expressions Quest Ledger 0.3 can evaluate in Minecraft 26.2.

## Evaluation lifecycle

Active quests are evaluated every five client ticks.

1. The current singleplayer world or multiplayer server scope is resolved.
2. The completion expression is evaluated against the current local player, world, statistic baselines, and manual confirmation state.
3. If the expression is true, completion starts immediately.
4. A fixed internal 700 ms completion effect plays.
5. The quest is removed and the completion timestamp, quest ID, and title are appended to the current scope's `completed-history.log`.

There is no configurable `hold` or removal timer. Legacy timer fields can still be parsed from older files, but runtime evaluation ignores them and user-facing QuestScript does not output them.

A runtime value that is not implemented evaluates as unknown, never as true. Unknown conditions are logged once instead of silently completing or deleting a quest.

Switching to another world or server immediately unloads the previous scope and clears any active completion effect. Returning to that scope reloads its own quests, statistic baselines, and manual confirmation state.

## Completion modes

### Automatic

The completion expression contains only measurable values. Examples include combat statistics, mining, item possession, location, dimension, health, hunger, and supported logical combinations.

### Manual

A manual quest contains `manual.checked` and no other measurable requirement.

```questscript
quest "금 공장 완성" {
  complete when {
    manual.checked == true
  }
}
```

Press **Confirm Complete** on the Active Quests screen. The confirmation is persisted in `manual-state.properties`.

### Hybrid

A hybrid quest contains `manual.checked` and one or more automatic conditions.

```questscript
quest "금 공장 가동 확인" {
  complete when {
    manual.checked == true
    and inventory.count("minecraft:gold_ingot") >= 64
  }
}
```

Manual confirmation satisfies only `manual.checked`; every remaining condition must also be true.

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

Nested expressions and parentheses are supported through the QuestScript AST.

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
| `manual.checked` | Persistent confirmation for this quest in the current scope |

## Supported inventory functions

| Function | Behavior |
|---|---|
| `inventory.count(id)` | Counts matching inventory items |
| `inventory.has(id)` | True when at least one matching item exists |
| `inventory.equipped(id)` | Checks all equipment slots, including hands |
| `inventory.durability(id)` | Remaining durability of the first matching damageable stack |

Inventory functions inspect the current inventory and are absolute, not creation-relative.

Item tags such as `#minecraft:logs` are parsed but are not evaluated yet. Use an exact namespaced item ID for automatic completion.

## Supported statistic functions

| Function | Value returned at runtime |
|---|---|
| `stat.mined(block_id)` | Blocks mined since this quest was created |
| `stat.used(item_id)` | Items used since this quest was created |
| `stat.crafted(item_id)` | Items crafted since this quest was created |
| `stat.killed(entity_id)` | Entities killed since this quest was created |
| `stat.picked_up(item_id)` | Items picked up since this quest was created |
| `stat.dropped(item_id)` | Items dropped since this quest was created |

When a quest is saved, Quest Ledger snapshots every vanilla statistic referenced by its completion expression. Runtime evaluation subtracts that saved baseline from the current vanilla value.

For example:

```questscript
quest "고대 잔해 네 개 더" {
  complete when {
    stat.mined("minecraft:ancient_debris") >= 4
  }
}
```

If the player had already mined 100 ancient debris when the quest was created, the expression begins at `0` and completes when the vanilla total reaches 104.

Baselines are stored in `runtime-state.properties`, survive restarts, and are removed with their quest. If a server resets a vanilla statistic below its stored baseline, Quest Ledger safely rebases that statistic to the new value instead of producing a negative count or completing incorrectly.

## Supported location functions

```questscript
distance.to(100, 64, -30) <= 5
inside.box(0, 60, 0, 32, 90, 32) == true
inside.radius(100, 64, -30, 8) == true
```

`quest.active(id_or_title)` is also available and checks the current scope's active quest store.

## Deferred runtime expressions

The following syntax validates but currently evaluates as unknown:

- `world.time`
- `world.day`
- `world.is_day`
- `world.is_night`
- `advancement.done(...)`
- `quest.done(...)`
- item tags
- biome, weather, difficulty, and game-mode properties

Minecraft 26.2 introduced named World Clocks and Timelines. Quest Ledger will add explicit clock and timeline arguments rather than pretending that one global day-time value still exists.

## Per-world and per-server storage

Each singleplayer save and multiplayer server receives an independent directory:

```text
config/quest-ledger/worlds/<readable-name>-<scope-hash>/
  scope.properties
  quests.qs
  runtime-state.properties
  manual-state.properties
  completed-history.log
```

Singleplayer scopes are keyed by the normalized world save path. Multiplayer scopes are keyed by the normalized server address. The readable directory prefix is only for convenience; the hash prevents collisions between worlds or servers with the same display name.

- `scope.properties` records the exact scope key, display name, and kind.
- `runtime-state.properties` stores statistic baselines.
- `manual-state.properties` stores manual confirmations that have not completed yet.

The internal state files should not normally be edited by hand.

## Migration from earlier versions

The former client-wide files:

```text
config/quest-ledger/quests.qs
config/quest-ledger/completed-history.log
```

are migrated into the first world or server opened after upgrading. The original files are archived under:

```text
config/quest-ledger/legacy/
```

A `legacy-migration.properties` marker prevents the same quests from being copied into every later scope.

Older versions did not record creation-time statistic baselines. Therefore, migrated statistic quests begin counting from the moment they are first loaded by the upgraded mod. Inventing an earlier baseline would risk accidental completion, so the migration deliberately chooses the safe behavior.

Quest files containing old `hold` or `remove after` fields remain readable. Those values no longer affect completion, and opening or formatting the quest in the editor produces timer-free user-facing source.
