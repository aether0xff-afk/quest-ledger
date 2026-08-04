# Editor modes

## Builder mode

Builder mode is the default editor for ordinary use. Quest Ledger 0.3 provides simple Builder conditions for:

- current inventory item count;
- blocks mined after the quest was created;
- mobs killed after the quest was created;
- manual completion confirmation.

A manual quest disables the target, comparison, and amount controls because its condition is simply:

```questscript
manual.checked == true
```

More complex nested conditions are written in QuestScript mode. Builder mode will refuse a lossy conversion when the current expression is outside its supported subset.

## QuestScript mode

QuestScript mode accepts complete QuestScript source and validates it before saving. It supports nested `AND`, `OR`, `NOT`, comparisons, player properties, inventory functions, statistics, location functions, and `manual.checked`.

The user-facing formatter deliberately omits timers. Legacy `hold` and `remove after` fields can still be read from old files, but they are not shown after formatting and have no runtime effect.

## Active Quests

The ledger footer opens an Active Quests screen.

Each quest is classified as:

- **Automatic** — all conditions are measured by the game;
- **Manual** — completion relies on `manual.checked`;
- **Hybrid** — `manual.checked` is combined with automatic conditions.

Automatic quests show a disabled **Automatic** label. Manual and hybrid quests show **Confirm Complete** until manual confirmation has been recorded.

For a hybrid quest, pressing the button does not force completion. It only makes `manual.checked` true; all other conditions still have to be satisfied.

Manual confirmations are stored per world or server in `manual-state.properties` and survive restarts until the quest completes.

## Quest Types guide

The Active Quests screen links to an in-game guide that explains:

- combat, mining, item, crafting/action, location, and compound automatic quests;
- builds and subjective projects that need manual completion;
- hybrid examples such as confirming a gold farm and also holding 64 gold ingots.

## Switching modes

When switching from QuestScript to Builder mode:

1. parse source;
2. validate types and names;
3. verify that the expression belongs to the current Builder subset;
4. if valid, import it into Builder controls;
5. otherwise remain in code mode without losing source.

Builder mode always produces a valid AST, so switching to QuestScript formats it into timer-free user-facing source.

## Medieval visual direction

- thin parchment rather than a large opaque book;
- dark brown ink and restrained brass accents;
- wax-seal animation on completion;
- page-fold and slide-out HUD removal;
- Minecraft font for readability;
- no raw OpenGL or Vulkan calls; use the current GUI/Blaze3D rendering layer.
