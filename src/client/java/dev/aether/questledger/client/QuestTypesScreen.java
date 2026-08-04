package dev.aether.questledger.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class QuestTypesScreen extends Screen {
    private static final int PANEL_COLOR = 0xFFF0D9A4;
    private static final int PANEL_DARK = 0xFF5B3A24;
    private static final int PANEL_MID = 0xFF9A6A3B;
    private static final int INK = 0xFF2B1A12;
    private static final int MUTED_INK = 0xFF6F5139;
    private static final int AUTO_INK = 0xFF355B2A;
    private static final int MANUAL_INK = 0xFF7A3528;

    private final Screen parent;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public QuestTypesScreen(Screen parent) {
        super(Component.translatable("screen.questledger.types.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(610, Math.max(340, this.width - 32));
        this.panelHeight = Math.min(390, Math.max(280, this.height - 32));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.back"),
                button -> onClose()
        ).bounds(
                this.panelLeft + this.panelWidth - 112,
                this.panelTop + this.panelHeight - 31,
                92,
                20
        ).build());
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(
                this.panelLeft + 5,
                this.panelTop + 6,
                this.panelLeft + this.panelWidth + 5,
                this.panelTop + this.panelHeight + 6,
                0x66000000
        );
        graphics.fill(
                this.panelLeft,
                this.panelTop,
                this.panelLeft + this.panelWidth,
                this.panelTop + this.panelHeight,
                PANEL_DARK
        );
        graphics.fill(
                this.panelLeft + 3,
                this.panelTop + 3,
                this.panelLeft + this.panelWidth - 3,
                this.panelTop + this.panelHeight - 3,
                PANEL_COLOR
        );
        graphics.fill(
                this.panelLeft + 14,
                this.panelTop + 42,
                this.panelLeft + this.panelWidth - 14,
                this.panelTop + 44,
                PANEL_MID
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.title);
        graphics.text(
                this.font,
                this.title,
                this.panelLeft + (this.panelWidth - titleWidth) / 2,
                this.panelTop + 13,
                INK,
                false
        );

        int x = this.panelLeft + 24;
        int y = this.panelTop + 55;
        drawSection(graphics, x, y, "screen.questledger.types.auto.title", AUTO_INK);
        y += 18;
        y = drawLines(graphics, x + 10, y, "screen.questledger.types.auto", 6);

        y += 7;
        drawSection(graphics, x, y, "screen.questledger.types.manual.title", MANUAL_INK);
        y += 18;
        y = drawLines(graphics, x + 10, y, "screen.questledger.types.manual", 4);

        y += 7;
        drawSection(graphics, x, y, "screen.questledger.types.hybrid.title", INK);
        y += 18;
        drawLines(graphics, x + 10, y, "screen.questledger.types.hybrid", 2);
    }

    private void drawSection(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String key,
            int color
    ) {
        graphics.text(
                this.font,
                Component.translatable(key),
                x,
                y,
                color,
                false
        );
    }

    private int drawLines(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String prefix,
            int count
    ) {
        for (int index = 1; index <= count; index++) {
            graphics.text(
                    this.font,
                    Component.translatable(prefix + "." + index),
                    x,
                    y,
                    MUTED_INK,
                    false
            );
            y += 14;
        }
        return y;
    }

    private void show(Screen screen) {
        this.minecraft.gui.setScreen(screen);
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
