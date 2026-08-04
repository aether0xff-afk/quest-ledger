package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Temporary CI-only visual smoke-test controller. It is completely inert unless
 * QUEST_LEDGER_VISUAL_SMOKE_TEST=1 is present in the process environment.
 */
public final class VisualSmokeTestController {
    private static final boolean ENABLED = "1".equals(
            System.getenv("QUEST_LEDGER_VISUAL_SMOKE_TEST")
    );

    private static int ticks;

    private VisualSmokeTestController() {
    }

    public static void tick(Minecraft client) {
        if (!ENABLED) {
            return;
        }

        ticks++;
        if (ticks == 80) {
            client.gui.setScreen(new QuestLedgerScreen(null));
        } else if (ticks == 140) {
            marker("quest-ledger-builder-ready");
        } else if (ticks == 300) {
            client.gui.setScreen(new QuestTypesScreen(null));
        } else if (ticks == 360) {
            marker("quest-ledger-types-ready");
        } else if (ticks == 560) {
            client.stop();
        }
    }

    private static void marker(String fileName) {
        try {
            Files.writeString(Path.of(fileName), "ready\n");
            QuestLedger.LOGGER.info("Visual smoke-test marker ready: {}", fileName);
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not write visual smoke-test marker", exception);
        }
    }
}
