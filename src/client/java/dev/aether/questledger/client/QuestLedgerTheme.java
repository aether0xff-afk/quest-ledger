package dev.aether.questledger.client;

import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Crisp pixel-art visual language shared by the ledger screens and HUD. */
final class QuestLedgerTheme {
    static final int BACKDROP = 0xB80A0908;
    static final int SHADOW = 0x88000000;
    static final int OUTLINE = 0xFF160E09;
    static final int WOOD_DARK = 0xFF2A190F;
    static final int LEATHER = 0xFF3B2416;
    static final int LEATHER_LIGHT = 0xFF674123;
    static final int WOOD_HIGHLIGHT = 0xFF8B6132;
    static final int GOLD_SHADOW = 0xFF654010;
    static final int GOLD_DARK = 0xFF93621F;
    static final int GOLD = 0xFFD29A35;
    static final int GOLD_LIGHT = 0xFFFFD76D;
    static final int PAPER_SHADOW = 0xFFC5A46B;
    static final int PAPER_ALT = 0xFFE3C994;
    static final int PAPER = 0xFFF2DEAE;
    static final int PAPER_LIGHT = 0xFFFFEDC4;
    static final int PAPER_CARD = 0xFFF6E5BC;
    static final int PAPER_CARD_HOVER = 0xFFFFF0C9;
    static final int INK = 0xFF24170F;
    static final int MUTED = 0xFF74563A;
    static final int GREEN = 0xFF2F6B36;
    static final int RED = 0xFF9A3027;
    static final int RED_DARK = 0xFF571B18;
    static final int RED_LIGHT = 0xFFD45842;
    static final int BLUE = 0xFF286A94;
    static final int ORANGE = 0xFF9A5A1A;

    private QuestLedgerTheme() {
    }

    static void drawBackdrop(GuiGraphicsExtractor graphics, QuestLedgerUiLayout.Frame frame) {
        int screenWidth = frame.right() + frame.left();
        int screenHeight = frame.bottom() + frame.top();
        graphics.fill(0, 0, screenWidth, screenHeight, BACKDROP);

        int left = frame.left();
        int top = frame.top();
        int right = frame.right();
        int bottom = frame.bottom();

        graphics.fill(left + 5, top + 6, right + 7, bottom + 8, SHADOW);
        stepped(graphics, left - 2, top - 2, right + 2, bottom + 2, OUTLINE, 3);
        stepped(graphics, left, top, right, bottom, WOOD_DARK, 3);
        stepped(graphics, left + 3, top + 3, right - 3, bottom - 3, GOLD_SHADOW, 2);
        stepped(graphics, left + 5, top + 5, right - 5, bottom - 5, GOLD, 2);
        stepped(graphics, left + 7, top + 7, right - 7, bottom - 7, LEATHER, 2);
        stepped(graphics, left + 10, top + 10, right - 10, bottom - 10, PAPER_SHADOW, 2);
        stepped(graphics, left + 12, top + 12, right - 12, bottom - 12, PAPER, 1);

        graphics.fill(left + 16, top + 14, right - 16, top + 15, PAPER_LIGHT);
        graphics.fill(left + 16, bottom - 16, right - 16, bottom - 14, 0x55725232);
        graphics.fill(left + 14, top + 18, left + 15, bottom - 18, 0x55FFFFFF);
        graphics.fill(right - 16, top + 18, right - 14, bottom - 18, 0x44725232);

        corner(graphics, left + 2, top + 2, 1, 1);
        corner(graphics, right - 2, top + 2, -1, 1);
        corner(graphics, left + 2, bottom - 2, 1, -1);
        corner(graphics, right - 2, bottom - 2, -1, -1);
        bookmark(graphics, left + 17, top - 1);
    }

    static void drawHeader(
            GuiGraphicsExtractor graphics,
            QuestLedgerUiLayout.Frame frame,
            int headerBottom
    ) {
        int left = frame.left() + 14;
        int right = frame.right() - 14;
        graphics.fill(left, frame.top() + 13, right, headerBottom, 0x16FFFFFF);
        rule(graphics, left + 10, right - 10, headerBottom - 2);
        diamond(graphics, left + 4, headerBottom - 4, GOLD_DARK);
        diamond(graphics, right - 8, headerBottom - 4, GOLD_DARK);
    }

    static void drawFooter(
            GuiGraphicsExtractor graphics,
            QuestLedgerUiLayout.Frame frame,
            int footerTop
    ) {
        int left = frame.left() + 14;
        int right = frame.right() - 14;
        rule(graphics, left + 10, right - 10, footerTop);
        graphics.fill(left, footerTop + 2, right, frame.bottom() - 13, 0x14000000);
    }

    static void drawCard(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            boolean emphasized
    ) {
        graphics.fill(left + 2, top + 3, right + 3, bottom + 3, 0x44000000);
        stepped(graphics, left, top, right, bottom, GOLD_SHADOW, 2);
        stepped(graphics, left + 1, top + 1, right - 1, bottom - 1,
                emphasized ? PAPER_CARD_HOVER : PAPER_CARD, 1);
        graphics.fill(left + 4, top + 3, right - 4, top + 4, 0x88FFFFFF);
        graphics.fill(left + 4, bottom - 3, right - 4, bottom - 2, 0x66745339);
        graphics.fill(left + 4, top + 5, left + 6, bottom - 5, 0x6693621F);
    }

    static void drawInset(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom
    ) {
        stepped(graphics, left, top, right, bottom, GOLD_SHADOW, 2);
        stepped(graphics, left + 2, top + 2, right - 2, bottom - 2, PAPER_ALT, 1);
        graphics.fill(left + 4, top + 4, right - 4, top + 5, PAPER_LIGHT);
        graphics.fill(left + 4, bottom - 5, right - 4, bottom - 4, 0x55745339);
    }

    static void drawButton(
            GuiGraphicsExtractor graphics,
            Font font,
            Button button,
            int mouseX,
            int mouseY
    ) {
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        boolean hovered = button.active
                && mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
        String label = button.getMessage().getString();
        Kind kind = kind(label);

        if (kind == Kind.TAB) {
            tab(graphics, font, button, label, hovered);
            return;
        }

        boolean primary = kind == Kind.PRIMARY;
        int outer;
        int inner;
        int highlight;
        int lowlight;
        int text;

        if (!button.active) {
            outer = 0xFF81735F;
            inner = PAPER_ALT;
            highlight = 0x99FFF2D0;
            lowlight = 0x55705E49;
            text = MUTED;
        } else if (primary) {
            outer = hovered ? GOLD_LIGHT : GOLD;
            inner = hovered ? RED_LIGHT : RED;
            highlight = 0xFFFF806A;
            lowlight = RED_DARK;
            text = 0xFFFFF0CC;
        } else {
            outer = hovered ? GOLD_LIGHT : GOLD_DARK;
            inner = hovered ? WOOD_HIGHLIGHT : LEATHER_LIGHT;
            highlight = hovered ? GOLD : WOOD_HIGHLIGHT;
            lowlight = WOOD_DARK;
            text = 0xFFFFE9B8;
        }

        graphics.fill(x + 2, y + 3, x + width + 3, y + height + 3, 0x55000000);
        stepped(graphics, x, y, x + width, y + height, OUTLINE, 2);
        stepped(graphics, x + 1, y + 1, x + width - 1, y + height - 1, outer, 1);
        stepped(graphics, x + 2, y + 2, x + width - 2, y + height - 2, inner, 1);
        graphics.fill(x + 4, y + 3, x + width - 4, y + 4, highlight);
        graphics.fill(x + 4, y + height - 4, x + width - 4, y + height - 3, lowlight);

        if (primary && width >= 70) {
            seal(graphics, x + width / 2, y + height - 1);
        }
        buttonText(graphics, font, label, x, y, width, height, text);
    }

    private static void tab(
            GuiGraphicsExtractor graphics,
            Font font,
            Button button,
            String label,
            boolean hovered
    ) {
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        boolean selected = !button.active;
        int outer = selected ? GOLD : GOLD_SHADOW;
        int inner = selected ? PAPER_LIGHT : (hovered ? WOOD_HIGHLIGHT : WOOD_DARK);
        int text = selected ? INK : 0xFFFFE9B8;

        stepped(graphics, x, y, x + width, y + height, OUTLINE, 2);
        stepped(graphics, x + 1, y + 1, x + width - 1, y + height - 1, outer, 1);
        stepped(graphics, x + 2, y + 2, x + width - 2, y + height - (selected ? 0 : 2), inner, 1);
        graphics.fill(x + 5, y + 2, x + width - 5, y + 3,
                selected ? GOLD_LIGHT : 0xFF7E522C);
        if (selected) {
            graphics.fill(x + 3, y + height - 2, x + width - 3, y + height + 1, PAPER);
        }
        buttonText(graphics, font, label, x, y, width, height, text);
    }

    private static void buttonText(
            GuiGraphicsExtractor graphics,
            Font font,
            String raw,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        String label = QuestLedgerUiLayout.ellipsize(font::width, raw, Math.max(1, width - 12));
        int textX = x + Math.max(5, (width - font.width(label)) / 2);
        int textY = y + Math.max(1, (height - 8) / 2);
        graphics.text(font, Component.literal(label), textX, textY, color, false);
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
        stepped(graphics, x, y, x + width, y + 13, OUTLINE, 1);
        stepped(graphics, x + 1, y + 1, x + width - 1, y + 12, background, 1);
        graphics.fill(x + 3, y + 2, x + width - 3, y + 3, 0x88FFFFFF);
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

    private static Kind kind(String label) {
        if (label.equals("조건 구성") || label.equals("Condition Builder")
                || label.equals("QuestScript")
                || label.equals("자동") || label.equals("Automatic")
                || label.equals("수동") || label.equals("Manual")
                || label.equals("혼합") || label.equals("Hybrid")) {
            return Kind.TAB;
        }
        if (label.equals("퀘스트 추가") || label.equals("Add Quest")) {
            return Kind.PRIMARY;
        }
        return Kind.NORMAL;
    }

    private static void stepped(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            int color,
            int cut
    ) {
        if (right <= left || bottom <= top) {
            return;
        }
        int corner = Math.max(0, Math.min(cut,
                Math.min((right - left) / 2, (bottom - top) / 2)));
        graphics.fill(left + corner, top, right - corner, bottom, color);
        graphics.fill(left, top + corner, right, bottom - corner, color);
    }

    private static void rule(GuiGraphicsExtractor graphics, int left, int right, int y) {
        graphics.fill(left + 2, y, right - 2, y + 1, GOLD_DARK);
        graphics.fill(left + 10, y + 1, right - 10, y + 2, GOLD);
        graphics.fill(left + 22, y + 2, right - 22, y + 3, 0x55FFFFFF);
    }

    private static void corner(
            GuiGraphicsExtractor graphics,
            int anchorX,
            int anchorY,
            int dx,
            int dy
    ) {
        int x2 = anchorX + dx * 14;
        int y2 = anchorY + dy * 14;
        int left = Math.min(anchorX, x2);
        int right = Math.max(anchorX, x2);
        int top = Math.min(anchorY, y2);
        int bottom = Math.max(anchorY, y2);
        graphics.fill(left, top, right, bottom, GOLD_SHADOW);
        int innerX = anchorX + dx * 3;
        int innerY = anchorY + dy * 3;
        int innerX2 = anchorX + dx * 11;
        int innerY2 = anchorY + dy * 11;
        graphics.fill(Math.min(innerX, innerX2), Math.min(innerY, innerY2),
                Math.max(innerX, innerX2), Math.max(innerY, innerY2), GOLD);
        int pinX = anchorX + dx * 7;
        int pinY = anchorY + dy * 7;
        graphics.fill(pinX - 1, pinY - 1, pinX + 2, pinY + 2, GOLD_LIGHT);
    }

    private static void bookmark(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 13, y + 28, OUTLINE);
        graphics.fill(x + 2, y + 1, x + 11, y + 24, RED_DARK);
        graphics.fill(x + 3, y + 2, x + 10, y + 22, RED);
        graphics.fill(x + 4, y + 3, x + 5, y + 20, RED_LIGHT);
        graphics.fill(x + 3, y + 21, x + 6, y + 26, RED);
        graphics.fill(x + 7, y + 21, x + 10, y + 26, RED);
    }

    private static void seal(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        graphics.fill(centerX - 5, centerY - 3, centerX + 6, centerY + 4, RED_DARK);
        graphics.fill(centerX - 7, centerY - 1, centerX + 8, centerY + 2, RED_DARK);
        graphics.fill(centerX - 4, centerY - 2, centerX + 5, centerY + 3, RED);
        diamond(graphics, centerX - 2, centerY - 1, GOLD_LIGHT);
    }

    private static void diamond(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x + 3, y, x + 5, y + 8, color);
        graphics.fill(x + 1, y + 2, x + 7, y + 6, color);
        graphics.fill(x, y + 3, x + 8, y + 5, color);
    }

    private enum Kind {
        NORMAL,
        TAB,
        PRIMARY
    }
}
