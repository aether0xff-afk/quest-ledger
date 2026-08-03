# QuestScript v0.1

QuestScript is a small declarative language for Quest Ledger completion conditions.

## Example

```questscript
quest "네더 탐험 준비" {
  id "nether_expedition"
  description "고대 잔해를 찾고 안전하게 귀환할 준비를 한다."

  complete when {
    player.dimension == "minecraft:the_nether"
    and (
      stat.mined("minecraft:ancient_debris") >= 4
      or inventory.count("minecraft:netherite_ingot") >= 1
    )
  }

  hold 2s
  remove after 1200ms
  animation "wax_seal"
  hud show
}
```

## Quest fields

- `id "identifier"` — optional stable internal ID.
- `description "text"` — optional description.
- `complete when { expression }` — required completion expression.
- `hold 2s` — condition must remain true for the duration.
- `remove after 1200ms` — HUD removal delay after completion.
- `animation "wax_seal"` — completion animation.
- `hud show` or `hud hide` — whether to pin the quest to the HUD.

Supported duration units are `ms`, `s`, `m`, and `h`.

## Operators

Logical operators:

```questscript
and
or
not
```

Comparison operators:

```questscript
==  !=  >  >=  <  <=
```

Precedence from highest to lowest:

1. parentheses;
2. `not`;
3. comparisons;
4. `and`;
5. `or`.

Use parentheses whenever `and` and `or` are mixed.

## Properties

```questscript
player.health
player.max_health
player.hunger
player.experience_level
player.x
player.y
player.z
player.dimension
player.biome
player.gamemode
player.on_ground
player.is_sneaking
player.is_sprinting

world.day
world.time
world.weather
world.difficulty
world.is_day
world.is_night

manual.checked
```

## Functions

```questscript
inventory.count("minecraft:diamond")
inventory.has("#minecraft:logs")
inventory.equipped("minecraft:elytra")
inventory.durability("minecraft:diamond_pickaxe")

stat.mined("minecraft:ancient_debris")
stat.used("minecraft:ender_pearl")
stat.crafted("minecraft:chest")
stat.killed("minecraft:wither_skeleton")
stat.picked_up("minecraft:diamond")
stat.dropped("minecraft:cobblestone")

advancement.done("minecraft:story/mine_diamond")
quest.done("village_trading_hall")
quest.active("nether_expedition")

distance.to(820, 64, -140)
inside.box(0, 60, 0, 32, 90, 32)
inside.radius(0, 64, 0, 16)
```

Functions and properties not listed here are rejected. Minecraft registry values should use namespaced IDs such as `minecraft:diamond`; tags begin with `#`.

## Supported animations

```text
wax_seal
ink_check
page_fold
```

## Safety

QuestScript intentionally has no loops, variables, custom functions, file access, network access, commands, JVM calls, reflection, or arbitrary code evaluation.
