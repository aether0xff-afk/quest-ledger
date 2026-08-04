package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;

public final class QuestLedgerHud {
    private static final int HEADER_HEIGHT = 21;
    private static final int ROW_HEIGHT = 20;

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

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int panelWidth = Math.min(
                Math.max(150, screenWidth - 16),
                QuestLedgerUiLayout.clamp(screenWidth / 3, 194, 286)
        );
        float entranceProgress = Math.min(
                1.0F,
                Math.max(
                        0.0F,
                        (Util.getMillis() - ClientQuestStore.changedAtMillis()) / 280.0F
                )
        );
        float eased = 1.0F - (1.0F - entranceProgress) * (1.0F - entranceProgress);
        int x = -panelWidth + 8 + Math.round(panelWidth * eased);
        int y = 8;
        int rows = visible.size() + (completion.isPresent() ? 1 : 0);
        int panelHeight = HEADER_HEIGHT + 7 + rows * ROW_HEIGHT;

        drawPanel(graphics, x, y, panelWidth, panelHeight);
        String header = QuestLedgerUiLayout.ellipsize(
                minecraft.font::width,
                Component.translatable("hud.questledger.title").getString(),
                panelWidth - 24
        );
        graphics.text(
                minecraft.font,
                Component.literal(header),
                x + 11,
                y + 7,
                QuestLedgerTheme.INK,
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
                    panelWidth - 16
            );
            rowY += ROW_HEIGHT;
        }

        for (QuestDefinition quest : visible) {
            drawQuestRow(graphics, minecraft, quest, x + 8, rowY, panelWidth - 16);
            rowY += ROW_HEIGHT;
        }
    }

    private static void drawPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.fill(x + 4, y + 5, x + width + 4, y + height + 5, 0x66000000);
        graphics.fill(x, y, x + width, y + height, QuestLedgerTheme.LEATHER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, QuestLedgerTheme.GOLD_DARK);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xF2EED5A5);
        graphics.fill(x + 10, y + HEADER_HEIGHT, x + width - 10, y + HEADER_HEIGHT + 1,
                QuestLedgerTheme.GOLD_DARK);
    }

    private static void drawQuestRow(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int width
    ) {
        QuestExpressionInspector.CompletionMode mode = QuestExpressionInspector.completionMode(quest);
        int accent = switch (mode) {
            case AUTOMATIC -> QuestLedgerTheme.GREEN;
            case MANUAL -> QuestLedgerTheme.RED;
            case HYBRID -> QuestLedgerTheme.BLUE;
        };
        graphics.fill(x, y, x + width, y + 16, 0x24FFFFFF);
        graphics.fill(x, y, x + 3, y + 16, accent);

        String title = QuestLedgerUiLayout.ellipsize(
                minecraft.font::width,
                quest.title(),
                width - 15
        );
        graphics.text(
                minecraft.font,
                Component.literal(title),
                x + 9,
                y + 4,
                QuestLedgerTheme.INK,
                false
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

        int paperAlpha = Math.round(190.0F * fade);
        graphics.fill(
                x,
                rowY,
                x + width,
                rowY + 16,
                withAlpha(0xF0D89C, paperAlpha)
        );

        String animation = view.quest().animation().orElse("wax_seal");
        switch (animation) {
            case "ink_check" -> drawInkCheck(graphics, minecraft, view.quest(), x, rowY, width, alpha);
            case "page_fold" -> drawPageFold(graphics, minecraft, view.quest(), x, rowY, width, alpha, progress);
            default -> drawWaxSeal(graphics, minecraft, view.quest(), x, rowY, width, alpha, progress);
        }
    }

    private static void drawWaxSeal(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int width,
            int alpha,
            float progress
    ) {
        float stampProgress = Math.min(1.0F, progress / 0.28F);
        int radius = Math.max(1, Math.round(5.0F * stampProgress));
        int centerX = x + 8;
        int centerY = y + 8;
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
        drawCompletedTitle(graphics, minecraft, quest, x + 18, y + 4, width - 21, alpha,
                QuestLedgerTheme.INK);
    }

    private static void drawInkCheck(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int width,
            int alpha
    ) {
        drawCompletedTitle(graphics, minecraft, quest, x + 6, y + 4, width - 9, alpha,
                QuestLedgerTheme.GREEN);
        graphics.fill(
                x + 6,
                y + 14,
                x + width - 6,
                y + 15,
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
        drawCompletedTitle(graphics, minecraft, quest, textX, y + 4,
                Math.max(10, x + width - textX - 5), alpha, QuestLedgerTheme.INK);
        int foldColor = withAlpha(0x6D4528, alpha);
        graphics.fill(x + width - fold, y, x + width - fold + 2, y + 16, foldColor);
    }

    private static void drawCompletedTitle(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            QuestDefinition quest,
            int x,
            int y,
            int maximumWidth,
            int alpha,
            int color
    ) {
        String title = QuestLedgerUiLayout.ellipsize(
                minecraft.font::width,
                "✓ " + quest.title(),
                maximumWidth
        );
        graphics.text(
                minecraft.font,
                Component.literal(title),
                x,
                y,
                withAlpha(color, alpha),
                false
        );
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }
}
