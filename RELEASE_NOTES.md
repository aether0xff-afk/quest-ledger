# Quest Ledger v0.4.0

Quest Ledger v0.4.0 is the complete visual and responsive-layout rebuild of the in-game ledger. The storage-safety guarantees introduced in 0.3.1 remain intact.

## Redesigned interface

- Replaced the prototype fixed-coordinate layout with one responsive layout system shared by every screen.
- Added a unified parchment, leather, and brass visual language for the editor, navigation, quest cards, badges, and HUD.
- Rebuilt the quest editor for compact and full-size GUI scales.
- Combined completion effect and HUD controls on very small screens to preserve a dedicated status and condition-preview area.
- Rebuilt Active Quests as adaptive cards with automatic, manual, and hybrid badges.
- Restored visible action buttons by enforcing background, content, and control rendering order.
- Replaced the long overflowing quest-type guide with a responsive tabbed card grid.
- Made the HUD width responsive and added safe truncation for long active and completed quest titles.
- Added deterministic ellipsis and wrapping rules for Korean and English text.

## Layout guarantees

The UI regression suite validates screen, editor, card, button, footer, and text bounds at these GUI resolutions:

- 320×240
- 360×270
- 426×240
- 640×360
- 854×480
- 1280×720
- 1920×1080

Compact screens display three quest cards per page and reserve independent space for editor status text, preventing the overlaps found in the 0.3 interface.

## Real-client verification

The repository now includes an inert CI-only visual smoke controller and workflow. For pull requests, GitHub Actions boots a real Minecraft 26.2 Fabric client with software rendering and captures the editor, Active Quests, and quest-type guide in both:

- standard profile: 1280×720, GUI scale 2;
- compact profile: 854×480, GUI scale 3.

The final v0.4.0 screens were inspected from these real-client captures after the initial visual run exposed and drove fixes for compact editor overlap, hidden quest action buttons, and guide/footer collision.

## Storage safety retained

- `quests.qs` and `runtime-state.properties` are committed as one recoverable transaction.
- Interrupted or failed saves are rolled back on the next scope load.
- Every quest has a persistent runtime ID.
- Existing manual confirmation keys migrate automatically.

## Automated verification

The release workflow runs the complete Java 25/Fabric build before publishing, including:

- Minecraft/Fabric main and client compilation;
- QuestScript parser and formatter self-tests;
- four transaction recovery scenarios;
- responsive UI layout and text-overflow regression tests;
- two real-client visual smoke profiles on pull requests.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Java 25

## Installation

Place `quest-ledger-0.4.0.jar` in the instance's `mods` directory and remove older Quest Ledger JARs first. Existing per-world quests, runtime baselines, and manual confirmations are retained and migrated automatically. Keeping a normal world backup before installing any new mod build is still recommended.
