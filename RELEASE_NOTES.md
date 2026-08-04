# Quest Ledger v0.3.1

Quest Ledger v0.3.1 is a storage-safety hotfix for the 0.3 manual and hybrid quest release.

## Fixed

- `quests.qs` and `runtime-state.properties` are now committed as one recoverable transaction.
- Interrupted or failed saves are rolled back on the next scope load instead of leaving mixed quest/runtime generations.
- Every quest now receives a persistent internal runtime ID.
- Manual completion confirmations no longer depend on duplicate quest ordering.
- Existing manual confirmation keys are migrated automatically to the new runtime-ID format.
- In-memory quest state is updated only after both persistent files have committed successfully.

## Verification

The release workflow runs the complete Java 25/Fabric build before publishing, including:

- Minecraft/Fabric main and client compilation;
- QuestScript parser and formatter self-tests;
- four transaction scenarios covering normal commits, checked I/O failure rollback, simulated process interruption recovery, and first-time storage creation.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3
- Fabric API 0.156.0+26.2
- Java 25

## Installation

Place `quest-ledger-0.3.1.jar` in the instance's `mods` directory. Remove older Quest Ledger JARs first. Existing per-world quests and manual confirmations are migrated automatically; keeping a normal backup before installing any new mod build is still recommended.
