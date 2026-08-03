# Architecture

## Core rule

Builder mode and QuestScript mode are two views of the **same typed AST**.

```text
QuestScript source ──parse──▶ typed AST ◀──edit── Builder UI
       ▲                           │                    │
       └──────────format───────────┘                    │
                                   ▼                    │
                          condition evaluator ◀─────────┘
```

No feature may be added to one editing mode unless it can be represented by the shared AST and edited in the other mode.

## Planned modules

1. `questscript` — lexer, parser, AST, validation, formatter.
2. `model` — quest IDs, status, progress, history, and persistence DTOs.
3. `condition` — event dependency analysis and runtime evaluation.
4. `client/editor` — builder and code editor tabs.
5. `client/hud` — pinned quest display and completion animation.
6. `network` — optional server-authoritative tracking for multiplayer.

## Runtime safety

QuestScript is interpreted data, not executable JVM or JavaScript code. It has no loops, user functions, reflection, file access, network access, commands, or arbitrary NBT mutation.

Processing order:

```text
source → lexer → parser → semantic validation → AST → evaluator
```

Only a validated AST may be persisted or evaluated.

## Evaluation plan

The AST will be statically inspected to discover its event dependencies.

- inventory functions → inventory change events;
- mining statistics → block break events;
- kill statistics → entity kill events;
- dimension properties → dimension change events;
- position functions → periodic sampling;
- advancement functions → advancement completion events;
- quest references → quest status change events.

This prevents every quest from being fully evaluated every game tick.
