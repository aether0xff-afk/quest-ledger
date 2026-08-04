# QuestScript v0.2

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

  animation "wax_seal"
  hud show
}
```

## Quest fields

- `id "identifier"` — optional stable internal ID. IDs are strongly recommended for manual and linked quests.
- `description "text"` — optional description.
- `complete when { expression }` — required completion expression.
- `animation "wax_seal"` — optional completion animation.
- `hud show` or `hud hide` — whether to pin the quest to the HUD.

QuestScript has no user-configurable completion timer. The expression is checked every five client ticks and completion begins immediately when it becomes true. A short fixed visual effect plays before the quest disappears.

Older files may contain `hold` or `remove after`. Those legacy fields remain readable for compatibility but are ignored by the runtime and omitted by the editor.

## Automatic, manual, and hybrid quests

### Automatic

```questscript
quest "위더 처치" {
  complete when {
    stat.killed("minecraft:wither") >= 1
  }
}
```

### Manual

```questscript
quest "금 공장 완성" {
  complete when {
    manual.checked == true
  }
}
```

Open the Active Quests screen and press **Confirm Complete**.

### Hybrid

```questscript
quest "금 공장 가동 확인" {
  complete when {
    manual.checked == true
    and inventory.count("minecraft:gold_ingot") >= 64
  }
}
```

Manual confirmation changes only `manual.checked`; all other conditions still have to be satisfied.

See [`QUEST_TYPES.md`](QUEST_TYPES.md) for detailed examples.

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

`manual.checked` is implemented. Biome, game mode, weather, difficulty, and World Clock properties are currently validation-only and evaluate as unknown at runtime.

## Functions

```questscript
inventory.count("minecraft:diamond")
inventory.has("minecraft:diamond")
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

Functions and properties not listed here are rejected. Minecraft registry values should use namespaced IDs such as `minecraft:diamond`.

Item tags beginning with `#` are accepted by the parser but are not yet evaluated automatically. Use exact resource IDs in active quests.

`advancement.done` and `quest.done` are validation-only in the current runtime. `quest.active` is implemented.

## Statistic versus inventory semantics

Statistic functions measure actions **after the quest was created**:

```questscript
stat.mined("minecraft:ancient_debris") >= 100
```

Inventory functions inspect the current inventory:

```questscript
inventory.count("minecraft:netherite_ingot") >= 100
```

Netherite ingots are items, not mined blocks. To express “mine netherite,” choose the actual block being mined, usually `minecraft:ancient_debris`. To express “obtain 100 netherite ingots,” use current inventory count or a suitable crafting/action statistic according to the intended goal.

## Supported animations

```text
wax_seal
ink_check
page_fold
```

## Safety

QuestScript intentionally has no loops, variables, custom functions, file access, network access, commands, JVM calls, reflection, or arbitrary code evaluation.
