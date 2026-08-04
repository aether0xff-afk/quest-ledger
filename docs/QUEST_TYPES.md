# Quest types

Quest Ledger divides quests into three practical completion modes.

## Automatic quests

Automatic quests can be measured directly from Minecraft client state or vanilla statistics. They complete as soon as their condition becomes true.

### Combat

Kill a wither after creating the quest:

```questscript
quest "위더 처치" {
  complete when {
    stat.killed("minecraft:wither") >= 1
  }

  animation "wax_seal"
}
```

The same form works for the Ender Dragon, zombies, wither skeletons, and other registered entity IDs.

### Mining

Mine 100 ancient debris after creating the quest:

```questscript
quest "고대 잔해 100개 채굴" {
  complete when {
    stat.mined("minecraft:ancient_debris") >= 100
  }
}
```

`stat.mined` counts blocks mined after the quest was created. Netherite ingots themselves are not mined blocks. A request such as “get 100 netherite” should therefore use an inventory or crafting condition instead.

### Current item possession

Hold 100 netherite ingots at the same time:

```questscript
quest "네더라이트 주괴 100개 확보" {
  complete when {
    inventory.count("minecraft:netherite_ingot") >= 100
  }
}
```

Inventory conditions inspect the current inventory. Dropping or spending items can make an unfinished condition false again.

### Item actions

```questscript
stat.crafted("minecraft:chest") >= 20
stat.used("minecraft:ender_pearl") >= 10
stat.picked_up("minecraft:diamond") >= 32
stat.dropped("minecraft:cobblestone") >= 100
```

These statistics count actions performed after the quest was created.

### Location and dimension

```questscript
player.dimension == "minecraft:the_nether"
distance.to(820, 64, -140) <= 5
inside.radius(0, 64, 0, 16) == true
inside.box(0, 60, 0, 32, 90, 32) == true
```

### Player state and compound rules

Health, hunger, experience, equipment, coordinates, dimensions, and supported functions can be joined with `and`, `or`, `not`, and parentheses.

```questscript
quest "안전하게 네더 귀환" {
  complete when {
    player.dimension == "minecraft:overworld"
    and player.health >= 16
    and inventory.count("minecraft:ancient_debris") >= 4
  }
}
```

## Manual quests

Some goals have no reliable generic game-state definition. A gold farm may have different designs, rates, dimensions, collection systems, and aesthetic requirements. Quest Ledger must not guess whether such a project is “finished.”

Use `manual.checked` for:

- finishing a gold farm, iron farm, mob farm, or villager trading hall;
- completing a house, road, bridge, storage room, or decorative build;
- organizing storage or improving a village;
- any subjective or project-level goal that Minecraft cannot measure directly.

```questscript
quest "금 공장 완성" {
  complete when {
    manual.checked == true
  }

  animation "page_fold"
}
```

Open **Active Quests** and press **Confirm Complete**. The confirmation is stored in that world or server's `manual-state.properties` until the full condition completes.

## Hybrid quests

Hybrid quests combine manual project confirmation with measurable evidence.

```questscript
quest "금 공장 가동 확인" {
  complete when {
    manual.checked == true
    and inventory.count("minecraft:gold_ingot") >= 64
  }

  animation "wax_seal"
}
```

Pressing **Confirm Complete** satisfies only `manual.checked`. The quest still waits until the inventory condition is true.

Another example:

```questscript
quest "네더 고속도로 구간 완성" {
  complete when {
    manual.checked == true
    and inside.radius(1000, 64, 0, 8) == true
  }
}
```

## Choosing the right condition

| Goal | Recommended mode | Condition |
|---|---|---|
| Kill one wither | Automatic | `stat.killed("minecraft:wither") >= 1` |
| Mine 100 ancient debris | Automatic | `stat.mined("minecraft:ancient_debris") >= 100` |
| Hold 100 netherite ingots | Automatic | `inventory.count("minecraft:netherite_ingot") >= 100` |
| Finish a gold farm | Manual | `manual.checked == true` |
| Finish a gold farm and prove it produced one stack | Hybrid | manual confirmation plus `inventory.count("minecraft:gold_ingot") >= 64` |
| Reach a destination | Automatic | `distance.to(...)`, `inside.radius(...)`, or `inside.box(...)` |
| Make a build look good | Manual | `manual.checked == true` |

Quest Ledger has no user-configurable completion timer. Conditions are checked every five client ticks and completion starts immediately when the expression becomes true. A short fixed visual effect plays before the completed quest disappears.
