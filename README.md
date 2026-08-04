# Quest Ledger

A personal quest and todo-list mod for **Minecraft Java Edition 26.2 + Fabric**.

Quest Ledger turns ordinary goals into lightweight in-game quests. Create them through the responsive visual builder or paste a typed QuestScript rule for more complex conditions. The mod does not contact or run an LLM; QuestScript can be written manually or generated externally and pasted into the ledger.

## Quest Ledger 0.4.3

- Press **K** to open the ledger. The binding is configurable in Minecraft Controls.
- Responsive parchment, leather, brass, and pixel-art screens for standard, compact, and micro GUI sizes.
- Visual builder conditions for current inventory, blocks mined after creation, mobs killed after creation, and manual confirmation.
- QuestScript parsing, type validation, runtime-support validation, and canonical formatting before save.
- Automatic, manual, and hybrid completion modes.
- Current player state, inventory, equipment, durability, exact-item statistics, locations, dimensions, `quest.active(...)`, and logical combinations.
- Per-world and per-server quest stores.
- Creation-relative statistic baselines that survive restarts.
- Periodic multiplayer statistic synchronization from the server.
- Persistent manual confirmations and completion history.
- Recoverable two-file transactions for quest source and runtime state.
- Three short completion effects: `wax_seal`, `ink_check`, and `page_fold`.
- Animated HUD showing up to three active quests.
- Korean and English localization.

## Creating a quest

### Visual builder

Open the ledger with **K**, choose a condition, enter a target ID and amount, select an effect and HUD visibility, then press **Add Quest**.

The builder currently supports:

- current item count;
- blocks mined since quest creation;
- mobs killed since quest creation;
- manual completion confirmation.

### QuestScript

```questscript
quest "고대 잔해 네 개 더" {
  id "four_more_debris"

  complete when {
    stat.mined("minecraft:ancient_debris") >= 4
  }

  animation "wax_seal"
  hud show
}
```

QuestScript mode supports more complex expressions:

```questscript
quest "금 공장 가동 확인" {
  id "gold_farm_verified"

  complete when {
    manual.checked == true
    and inventory.count("minecraft:gold_ingot") >= 64
    and inside.radius(120, 70, -35, 12) == true
  }

  animation "page_fold"
}
```

New saves are checked against the runtime implementation. Syntax reserved for future features remains readable in older files, but the editor rejects it with `UNSUPPORTED_RUNTIME` rather than saving a quest that cannot complete.

## Supported runtime expressions

### Player properties

- `player.health`
- `player.max_health`
- `player.hunger`
- `player.experience_level`
- `player.x`, `player.y`, `player.z`
- `player.dimension`
- `player.on_ground`
- `player.is_sneaking`
- `player.is_sprinting`
- `manual.checked`

### Inventory and equipment

- `inventory.count(id)`
- `inventory.has(id)`
- `inventory.equipped(id)`
- `inventory.durability(id)`

Use exact namespaced IDs such as `minecraft:diamond`. Tags such as `#minecraft:logs` are deferred.

### Creation-relative statistics

- `stat.mined(block_id)`
- `stat.used(item_id)`
- `stat.crafted(item_id)`
- `stat.killed(entity_id)`
- `stat.picked_up(item_id)`
- `stat.dropped(item_id)`

When a statistic quest is created, Quest Ledger records the current synchronized vanilla value as its baseline. If `stat.mined("minecraft:stone")` is 40 at creation, the expression reaches 3 when the server reports 43. Active multiplayer scopes request current statistics periodically so these baselines and deltas do not depend on stale client data.

### Location and quest state

- `distance.to(x, y, z)`
- `inside.box(x1, y1, z1, x2, y2, z2)`
- `inside.radius(x, y, z, radius)`
- `quest.active(id_or_title)`

### Operators

- `and`, `or`, `not`
- `==`, `!=`, `>`, `>=`, `<`, `<=`

## Deferred expressions

These remain parseable for compatibility but cannot be added as new quests yet:

- `world.time`, `world.day`, `world.is_day`, `world.is_night`;
- biome, weather, difficulty, and game-mode properties;
- `advancement.done(...)`;
- `quest.done(...)`;
- item and block tags.

Existing files containing deferred expressions are loaded safely as unknown conditions and are never completed or deleted by mistake.

## Completion behavior

Quest conditions are evaluated every five client ticks. When a condition becomes true, the selected fixed 700 ms effect plays, the quest is removed, and an entry is appended to the current scope's completion history.

There is no configurable completion timer. Legacy `hold` and `remove after` fields remain readable but are ignored by the runtime and omitted from user-facing formatted QuestScript.

## Storage

Each singleplayer world and multiplayer server gets an independent directory:

```text
config/quest-ledger/worlds/<scope-name>-<hash>/
  scope.properties
  quests.qs
  runtime-state.properties
  manual-state.properties
  completed-history.log
```

`quests.qs` and `runtime-state.properties` are committed as one recoverable transaction. Interrupted writes are rolled back on the next scope load, and the in-memory list changes only after both files commit successfully.

Quest instances have persistent runtime UUIDs. Manual confirmation keys from older versions are migrated automatically. The legacy client-wide store is moved into the first opened scope and archived under `config/quest-ledger/legacy/`.

## Verification

Every pull request runs:

- Minecraft/Fabric main, client, and test compilation on Java 25;
- 20,297 save-facing QuestScript feature and deterministic fuzz checks;
- the parser and formatter regression suite;
- 19 transaction scenarios covering every recoverable interruption point;
- responsive geometry and text-fitting checks across nine logical resolutions;
- real Minecraft visual smoke tests at standard, compact, and micro profiles;
- a real Fabric server and two consecutive client launches against the same multiplayer scope.

The server/client workflow verifies successful saves, duplicate-ID rejection, unsupported-runtime rejection, automatic and manual completion, `quest.active(...)`, non-zero synchronized statistic progress, effects, completion history, full client restart persistence, and store clearing.

CI-only controllers are inert during normal play and activate only through workflow environment variables.

## Requirements

- Minecraft Java Edition `26.2`
- Fabric Loader `0.19.3+`
- Fabric API `0.156.0+26.2`
- Java `25+`

Minecraft 26.2 may render through Vulkan or OpenGL. Quest Ledger uses Minecraft/Blaze3D and Fabric HUD abstractions rather than backend-specific graphics calls.

## Build

```shell
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

The remapped mod JAR is created in `build/libs/`.

Further documentation:

- [`docs/QUESTSCRIPT.md`](docs/QUESTSCRIPT.md)
- [`docs/RUNTIME_SUPPORT.md`](docs/RUNTIME_SUPPORT.md)
- [`docs/QUEST_TYPES.md`](docs/QUEST_TYPES.md)
- [`docs/EDITOR_MODES.md`](docs/EDITOR_MODES.md)
- [`docs/ASK_GPT.md`](docs/ASK_GPT.md)
- [`docs/RENDERING_BACKENDS.md`](docs/RENDERING_BACKENDS.md)

## Known deferred product features

- editing, deleting, and restoring quests from the management screen;
- a formatted completion-history screen;
- advancement, completed-quest dependency, tags, and world timeline conditions.

## License

MIT
