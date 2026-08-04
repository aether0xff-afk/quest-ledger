# Quest Ledger v0.4.2

Quest Ledger v0.4.2 is a compact-GUI layout hotfix for the pixel-art v0.4 interface. It keeps the same quest features and storage format while removing the remaining collisions seen on narrow or high-GUI-scale windows.

## Compact GUI fixes

- Reserved a protected preview/status band above the editor footer.
- Reduced the tiny editor row pitch while keeping every interactive control at least 20 pixels tall.
- Changed tiny Active Quests pages from three compressed cards to two taller, readable cards.
- Reduced compact action and navigation button widths without truncating Korean labels.
- Measured the exact gap between pagination arrows and the right-side footer buttons, then placed the page label only inside that gap.
- Prevented the quest-type guide from collapsing to a one-column, six-row layout that could overflow vertically.
- Reserved a larger footer band for the compact quest-type guide.

## Verification

The complete Java 25/Fabric build passes, including:

- Minecraft/Fabric main and client compilation;
- QuestScript parser and formatter self-tests;
- four recoverable storage transaction scenarios;
- responsive UI regression checks across nine logical resolutions;
- explicit editor widget/status/footer separation checks;
- compact list footer spacing checks;
- compact guide grid-height checks.

Real Minecraft 26.2 clients were launched and captured in three profiles:

- standard: 1280×720, GUI scale 2;
- compact: 854×480, GUI scale 3;
- micro: 720×405, requested GUI scale 3.

Minecraft automatically lowers GUI scale where necessary to preserve its own minimum readable dimensions. All three profiles rendered the editor, Active Quests list, and quest-type guide without text overflow, card collision, hidden controls, or footer overlap.

## Compatibility and storage

The quest format and per-world storage remain compatible with v0.4.0 and v0.4.1. Existing quests, runtime statistic baselines, manual confirmations, and completion history are retained.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Java 25

## Installation

Remove older Quest Ledger JARs and place `quest-ledger-0.4.2.jar` in the instance's `mods` directory. Keeping a normal world backup before installing any mod update is still recommended.
