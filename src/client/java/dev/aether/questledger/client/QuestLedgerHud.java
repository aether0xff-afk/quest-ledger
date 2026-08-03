package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;

public final class QuestLedgerHud {
    private static final int PANEL_WIDTH = 190;
    private static final int HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 16;

    private QuestLedgerHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden()) {
            return;
        }

        Optional<QuestCompletionController.CompletionView> completion =
                QuestCompletionController.completionView();
        List<QuestDefinition> visible = ClientQuestStore.snapshot().quests().stream()
                .filter(QuestDefinition::hudVisible)
                .filter(quest -> !QuestCompletionController.isCompleting(quest))
                .limit(completion.isPresent() ? 2 : 3)
                .toList();
        if (visible.isEmpty() && completion.isEmpty()) {
            return;
        }

        float entranceProgress = Math.min(
                1.0F,
                Math.max(
                        0.0F,
                        (Util.getMillis() - ClientQuestStore.changedAtMillis()) / 280.0F
                )
        );
        float eased = 1.0F - (1.0F - entranceProgress) * (1.0F - entranceProgress);
        int x = -PANEL_WIDTH + 8 + Math.round(PANEL_WIDTH * eased);
        int y = 8;
        int rows = visible.size() + (completion.isPresent() ? 1 : 0);
        int panelHeight = HEADER_HEIGHT + 8 + rows * ROW_HEIGHT;

        drawPanel(graphics, x, y, panelHeight);
        graphics.text(
                minecraft.font,
                Component.translatable("hud.questledger.title"),
                x + 9,
                y + 6,
                0xFF2B1A12,
                false
        );

        int rowY = y + HEADER_HEIGHT + 5;
        if (completion.isPresent()) {
            drawCompletion(
                    graphics,
                    minecraft,
                    completion.get(),
                    x + 8,
                    rowY,
                    PANEL_WIDTH - 16
            );
            rowY += ROW_HEIGHT;
        }

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

    private static void drawPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int panelHeight
    ) {
        graphics.fill(x + 3, y + 4, x + PANEL_WIDTH + 3, y + panelHeight + 4, 0x66000000);
        graphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xDD4B301E);
        graphics.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + panelHeight - 2, 0xEEDDBF82);
        graphics.fill(
                x + 8,
                y + HEADER_HEIGHT,
                x + PANEL_WIDTH - 8,
                y + HEADER_HEIGHT + 1,
                0xFF8A5B34
        );
    }

    private static void drawCompletion(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestCompletionController.CompletionView view,
            int x,
            int y,
            int width
    ) {
        float progress = view.progress();
        float fade = progress < 0.72F
                ? 1.0F
                : Math.max(0.0F, 1.0F - (progress - 0.72F) / 0.28F);
        int alpha = Math.round(255.0F * fade);
        int lift = Math.round(progress * 3.0F);
        int rowY = y - lift;

        int paperAlpha = Math.round(180.0F * fade);
        graphics.fill(
                x,
                rowY - 2,
                x + width,
                rowY + 12,
                withAlpha(0xC98F4A, paperAlpha)
        );

        String animation = view.quest().animation().orElse("wax_seal");
        switch (animation) {
            case "ink_check" -> drawInkCheck(graphics, minecraft, view.quest(), x, rowY, alpha);
            case "page_fold" -> drawPageFold(graphics, minecraft, view.quest(), x, rowY, width, alpha, progress);
            default -> drawWaxSeal(graphics, minecraft, view.quest(), x, rowY, alpha, progress);
        }
    }

    private static void drawWaxSeal(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int alpha,
            float progress
    ) {
        float stampProgress = Math.min(1.0F, progress / 0.28F);
        int radius = Math.max(1, Math.round(5.0F * stampProgress));
        int centerX = x + 7;
        int centerY = y + 5;
        int sealColor = withAlpha(0x8E241F, alpha);

        for (int offset = -radius; offset <= radius; offset++) {
            int halfWidth = radius - Math.abs(offset);
            graphics.fill(
                    centerX - halfWidth,
                    centerY + offset,
                    centerX + halfWidth + 1,
                    centerY + offset + 1,
                    sealColor
            );
        }
        graphics.text(
                minecraft.font,
                "✓ " + quest.title(),
                x + 17,
                y + 1,
                withAlpha(0x2B1A12, alpha),
                false
        );
    }

    private static void drawInkCheck(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int alpha
    ) {
        graphics.text(
                minecraft.font,
                "✓ " + quest.title(),
                x + 3,
                y + 1,
                withAlpha(0x245A2B, alpha),
                false
        );
        int lineWidth = Math.min(152, minecraft.font.width(quest.title()) + 2);
        graphics.fill(
                x + 16,
                y + 11,
                x + 16 + lineWidth,
                y + 12,
                withAlpha(0x245A2B, alpha)
        );
    }

    private static void drawPageFold(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int width,
            int alpha,
            float progress
    ) {
        int fold = Math.round(width * Math.min(1.0F, progress / 0.85F));
        int textX = x + Math.min(fold / 5, 12);
        graphics.text(
                minecraft.font,
                "✓ " + quest.title(),
                textX,
                y + 1,
                withAlpha(0x2B1A12, alpha),
                false
        );
        int foldColor = withAlpha(0x6D4528, alpha);
        graphics.fill(x + width - fold, y - 2, x + width - fold + 2, y + 12, foldColor);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }
}
