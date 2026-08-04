# Quest Ledger v0.4.1

Quest Ledger v0.4.1 is the final pixel-art polish pass for the responsive v0.4 interface. It keeps the same quest features and storage format while making the ledger look cleaner and more natural inside Minecraft.

## Pixel-art interface polish

- Replaced soft layered framing with crisp stepped 1–3 pixel borders.
- Tightened the palette to parchment, dark wood, leather, brass, ink, and wax red.
- Added hard-edged pixel shadows, brass corner plates, page highlights, and a red bookmark.
- Made the selected tab visually connect to the parchment page.
- Gave ordinary actions a restrained wood-and-brass treatment.
- Made `Add Quest` the clear primary action with a red wax-seal treatment.
- Refined card and badge borders without changing responsive geometry or text-overflow guarantees.

## Verification

The complete Java 25/Fabric build passes, including:

- Minecraft/Fabric main and client compilation;
- QuestScript parser and formatter self-tests;
- four recoverable storage transaction scenarios;
- responsive UI layout and text-overflow regression tests.

Real Minecraft 26.2 clients were also launched and inspected in both:

- standard profile: 1280×720, GUI scale 2;
- compact profile: 854×480, GUI scale 3.

The editor, Active Quests list, and quest-type guide were captured in both profiles. No text overflow, control collision, hidden action button, or footer overlap was found.

## Compatibility and storage

The quest format and per-world storage remain compatible with v0.4.0. Existing quests, runtime statistic baselines, manual confirmations, and completion history are retained.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Java 25

## Installation

Remove older Quest Ledger JARs and place `quest-ledger-0.4.1.jar` in the instance's `mods` directory. Keeping a normal world backup before installing any mod update is still recommended.
