# QuestScript runtime support

This document describes which QuestScript expressions Quest Ledger v0.3 can evaluate automatically in Minecraft 26.2.

## Evaluation lifecycle

Active quests are evaluated every five client ticks.

1. The completion expression is evaluated against the current local player and client world.
2. If it is false, any in-progress `hold` timer resets.
3. If it remains true for the quest's `hold` duration, completion animation begins.
4. The quest is removed after the greater of `remove after` and 450 ms.
5. The completion timestamp, quest ID, and title are appended to `completed-history.log`.

A runtime value that is not implemented evaluates as unknown, never as true. Unknown conditions are logged once instead of silently completing or deleting a quest.

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

## Supported inventory functions

| Function | Behavior |
|---|---|
| `inventory.count(id)` | Counts matching inventory items |
| `inventory.has(id)` | True when at least one matching item exists |
| `inventory.equipped(id)` | Checks all equipment slots, including hands |
| `inventory.durability(id)` | Remaining durability of the first matching damageable stack |

Item tags such as `#minecraft:logs` are parsed but are not evaluated yet. Use an exact namespaced item ID for automatic completion.

## Supported statistic functions

| Function | Vanilla statistic |
|---|---|
| `stat.mined(block_id)` | Blocks mined |
| `stat.used(item_id)` | Items used |
| `stat.crafted(item_id)` | Items crafted |
| `stat.killed(entity_id)` | Entities killed |
| `stat.picked_up(item_id)` | Items picked up |
| `stat.dropped(item_id)` | Items dropped |

These are the player's cumulative vanilla statistics. A quest created after the statistic already reached its threshold can complete immediately. A future baseline operator will support goals such as “mine four more blocks from now.”

## Supported location functions

```questscript
distance.to(100, 64, -30) <= 5
inside.box(0, 60, 0, 32, 90, 32) == true
inside.radius(100, 64, -30, 8) == true
```

`quest.active(id_or_title)` is also available and checks the current active client quest store.

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
- `manual.checked`

Minecraft 26.2 introduced named World Clocks and Timelines. Quest Ledger will add explicit clock and timeline arguments rather than pretending that one global day-time value still exists.

## Storage scope

Active quests are currently stored at:

```text
config/quest-ledger/quests.qs
```

Completion history is currently stored at:

```text
config/quest-ledger/completed-history.log
```

Both files are client-wide. Per-world and per-server profiles are planned before the first stable release.
