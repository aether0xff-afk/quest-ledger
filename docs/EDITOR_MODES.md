# Editor modes

## Builder mode

Builder mode is the default editor for ordinary use. Every condition is represented as a card.

A condition card contains:

- value source selector;
- Minecraft registry picker where applicable;
- comparison operator;
- numeric field and optional slider;
- live true/false state;
- delete, duplicate, negate, and drag handles.

Logical groups support `AND`, `OR`, and `NOT`. Groups can be nested, collapsed, reordered, and dragged into one another.

Sliders are convenience controls only. Every numeric value also has an exact text field, so large coordinates and counts are never limited by a slider range.

## QuestScript mode

QuestScript mode provides:

- syntax highlighting;
- automatic indentation and bracket pairing;
- property, function, resource-ID, and quest-ID completion;
- inline diagnostics;
- current-condition preview;
- format source command;
- copy prompt for GPT command;
- switch to Builder mode command.

## Switching modes

When switching from QuestScript to Builder mode:

1. parse source;
2. validate types and names;
3. if valid, render the resulting AST;
4. if invalid, remain in code mode and focus the first error.

Builder mode always produces valid AST, so switching to QuestScript simply formats it into canonical source.

## Medieval visual direction

- thin parchment rather than a large opaque book;
- dark brown ink and restrained brass accents;
- wax-seal animation on completion;
- page-fold and slide-out HUD removal;
- Minecraft font for readability;
- no raw OpenGL calls; use the current GUI/Blaze3D rendering layer.
