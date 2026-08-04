package dev.aether.questledger.client;

import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Shared visual language for the v0.4 ledger screens and HUD. */
final class QuestLedgerTheme {
    static final int BACKDROP = 0xC20B0A09;
    static final int SHADOW = 0x78000000;
    static final int LEATHER = 0xFF3A2418;
    static final int LEATHER_LIGHT = 0xFF68442B;
    static final int GOLD = 0xFFC49A52;
    static final int GOLD_DARK = 0xFF8C6330;
    static final int PAPER = 0xFFF4E2B7;
    static final int PAPER_ALT = 0xFFEAD2A0;
    static final int PAPER_CARD = 0xFFF9EBC9;
    static final int PAPER_CARD_HOVER = 0xFFFFF2D3;
    static final int INK = 0xFF24160F;
    static final int MUTED = 0xFF70553E;
    static final int GREEN = 0xFF355E35;
    static final int RED = 0xFF8C3028;
    static final int BLUE = 0xFF355C70;
    static final int ORANGE = 0xFF805020;

    private QuestLedgerTheme() {
    }

    static void drawBackdrop(
            GuiGraphicsExtractor graphics,
            QuestLedgerUiLayout.Frame frame
    ) {
        graphics.fill(0, 0, frame.right() + frame.left(), frame.bottom() + frame.top(), BACKDROP);

        int left = frame.left();
        int top = frame.top();
        int right = frame.right();
        int bottom = frame.bottom();

        graphics.fill(left + 7, top + 9, right + 7, bottom + 9, SHADOW);
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF1E120C);
        graphics.fill(left, top, right, bottom, LEATHER);
        graphics.fill(left + 3, top + 3, right - 3, bottom - 3, GOLD_DARK);
        graphics.fill(left + 5, top + 5, right - 5, bottom - 5, PAPER);
        graphics.fill(left + 8, top + 8, right - 8, bottom - 8, PAPER_ALT);
        graphics.fill(left + 10, top + 10, right - 10, bottom - 10, PAPER);

        // Leather corner guards and small brass pins.
        drawCorner(graphics, left + 5, top + 5, 1, 1);
        drawCorner(graphics, right - 5, top + 5, -1, 1);
        drawCorner(graphics, left + 5, bottom - 5, 1, -1);
        drawCorner(graphics, right - 5, bottom - 5, -1, -1);
    }

    static void drawHeader(
            GuiGraphicsExtractor graphics,
            QuestLedgerUiLayout.Frame frame,
            int headerBottom
    ) {
        int left = frame.left() + 10;
        int right = frame.right() - 10;
        int top = frame.top() + 10;
        graphics.fill(left, top, right, headerBottom, 0x33FFFFFF);
        graphics.fill(left + 8, headerBottom - 2, right - 8, headerBottom - 1, GOLD_DARK);
        graphics.fill(left + 24, headerBottom - 1, right - 24, headerBottom, GOLD);
    }

    static void drawFooter(
            GuiGraphicsExtractor graphics,
            QuestLedgerUiLayout.Frame frame,
            int footerTop
    ) {
        int left = frame.left() + 10;
        int right = frame.right() - 10;
        graphics.fill(left + 24, footerTop, right - 24, footerTop + 1, GOLD);
        graphics.fill(left + 8, footerTop + 1, right - 8, footerTop + 2, GOLD_DARK);
        graphics.fill(left, footerTop + 2, right, frame.bottom() - 10, 0x18000000);
    }

    static void drawCard(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            boolean emphasized
    ) {
        graphics.fill(left + 2, top + 3, right + 2, bottom + 3, 0x30000000);
        graphics.fill(left, top, right, bottom, GOLD_DARK);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1,
                emphasized ? PAPER_CARD_HOVER : PAPER_CARD);
        graphics.fill(left + 5, top + 5, left + 7, bottom - 5, 0x558C6330);
    }

    static void drawInset(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom
    ) {
        graphics.fill(left, top, right, bottom, LEATHER_LIGHT);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFFF8EAC8);
        graphics.fill(left + 4, top + 4, right - 4, bottom - 4, 0xFFF1DDB1);
    }

    static void drawBadge(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y,
            int width,
            int background,
            int foreground
    ) {
        graphics.fill(x, y, x + width, y + 13, 0x55000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 12, background);
        String fitted = QuestLedgerUiLayout.ellipsize(font::width, text.getString(), width - 8);
        int textX = x + Math.max(4, (width - font.width(fitted)) / 2);
        graphics.text(font, Component.literal(fitted), textX, y + 2, foreground, false);
    }

    static void drawSectionLabel(
            GuiGraphicsExtractor graphics,
            Font font,
            Component label,
            int x,
            int y,
            int maximumWidth,
            int color
    ) {
        String fitted = QuestLedgerUiLayout.ellipsize(font::width, label.getString(), maximumWidth);
        graphics.text(font, Component.literal(fitted), x, y, color, false);
    }

    static Component fitted(Font font, Component component, int width) {
        return Component.literal(QuestLedgerUiLayout.ellipsize(font::width, component.getString(), width));
    }

    private static void drawCorner(
            GuiGraphicsExtractor graphics,
            int anchorX,
            int anchorY,
            int xDirection,
            int yDirection
    ) {
        int x2 = anchorX + xDirection * 18;
        int y2 = anchorY + yDirection * 18;
        int left = Math.min(anchorX, x2);
        int right = Math.max(anchorX, x2);
        int top = Math.min(anchorY, y2);
        int bottom = Math.max(anchorY, y2);
        graphics.fill(left, top, right, bottom, LEATHER);
        int pinX = anchorX + xDirection * 7;
        int pinY = anchorY + yDirection * 7;
        graphics.fill(pinX - 1, pinY - 1, pinX + 2, pinY + 2, GOLD);
    }
}
