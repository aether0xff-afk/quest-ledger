package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class QuestLedgerHud {
    private static final int PANEL_WIDTH = 190;
    private static final int HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 14;

    private QuestLedgerHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        List<QuestDefinition> visible = ClientQuestStore.snapshot().quests().stream()
                .filter(QuestDefinition::hudVisible)
                .limit(3)
                .toList();
        if (visible.isEmpty()) {
            return;
        }

        float progress = Math.min(1.0F,
                Math.max(0.0F, (Util.getMillis() - ClientQuestStore.changedAtMillis()) / 280.0F));
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        int x = -PANEL_WIDTH + 8 + Math.round(PANEL_WIDTH * eased);
        int y = 8;
        int panelHeight = HEADER_HEIGHT + 8 + visible.size() * ROW_HEIGHT;

        graphics.fill(x + 3, y + 4, x + PANEL_WIDTH + 3, y + panelHeight + 4, 0x66000000);
        graphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xDD4B301E);
        graphics.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + panelHeight - 2, 0xEEDDBF82);
        graphics.fill(x + 8, y + HEADER_HEIGHT, x + PANEL_WIDTH - 8, y + HEADER_HEIGHT + 1, 0xFF8A5B34);

        graphics.text(
                minecraft.font,
                Component.translatable("hud.questledger.title"),
                x + 9,
                y + 6,
                0xFF2B1A12,
                false
        );

        int rowY = y + HEADER_HEIGHT + 5;
        for (QuestDefinition quest : visible) {
            graphics.text(
                    minecraft.font,
                    "◇ " + quest.title(),
                    x + 10,
                    rowY,
                    0xFF2B1A12,
                    false
            );
            rowY += ROW_HEIGHT;
        }
    }
}
