package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

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
            capture(client, "quest-ledger-builder.png", "quest-ledger-builder-ready");
        } else if (ticks == 260) {
            client.gui.setScreen(new QuestTypesScreen(null));
        } else if (ticks == 320) {
            capture(client, "quest-ledger-types.png", "quest-ledger-types-ready");
        } else if (ticks == 460) {
            client.stop();
        }
    }

    private static void capture(Minecraft client, String fileName, String markerName) {
        Screenshot.grab(
                client.gameDirectory,
                fileName,
                client.getMainRenderTarget(),
                1,
                message -> {
                    QuestLedger.LOGGER.info(
                            "Visual smoke-test screenshot result: {}",
                            message.getString()
                    );
                    marker(markerName);
                }
        );
    }

    private static void marker(String fileName) {
        try {
            Files.writeString(Path.of(fileName), "ready\n");
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not write visual smoke-test marker", exception);
        }
    }
}
