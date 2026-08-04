package dev.aether.questledger.client;

import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class QuestTypesScreen extends Screen {
    private static final int AUTO = 0;
    private static final int MANUAL = 1;
    private static final int HYBRID = 2;

    private final Screen parent;
    private final int section;
    private final List<Button> styledButtons = new ArrayList<>();
    private QuestLedgerUiLayout.Frame frame;
    private int contentTop;
    private int contentBottom;
    private int padding;

    public QuestTypesScreen(Screen parent) {
        this(parent, AUTO);
    }

    private QuestTypesScreen(Screen parent, int section) {
        super(Component.translatable("screen.questledger.types.title"));
        this.parent = parent;
        this.section = Math.max(AUTO, Math.min(HYBRID, section));
    }

    @Override
    protected void init() {
        this.styledButtons.clear();
        this.frame = QuestLedgerUiLayout.frame(this.width, this.height);
        this.padding = this.frame.compact() ? 12 : 18;
        this.contentTop = this.frame.top() + (this.frame.tiny() ? 56 : 64);
        // Reserve the same protected footer band used by the compact quest list.
        this.contentBottom = this.frame.bottom() - (this.frame.tiny() ? 44 : 44);

        addTabs();

        int backWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.back").getString(),
                this.frame.tiny() ? 58 : 70,
                104
        );
        addButton(Button.builder(
                Component.translatable("screen.questledger.back"),
                button -> onClose()
        ).bounds(
                this.frame.right() - this.padding - backWidth,
                this.frame.bottom() - 31,
                backWidth,
                20
        ).build());
    }

    private void addTabs() {
        int gap = 5;
        int available = this.frame.width() - this.padding * 2;
        int width = Math.max(58, (available - gap * 2) / 3);
        int x = this.frame.left() + this.padding;
        int y = this.frame.top() + 34;
        String[] keys = {
                "screen.questledger.mode.automatic",
                "screen.questledger.mode.manual",
                "screen.questledger.mode.hybrid"
        };

        for (int index = 0; index < keys.length; index++) {
            final int target = index;
            Button button = Button.builder(
                    Component.translatable(keys[index]),
                    ignored -> show(new QuestTypesScreen(this.parent, target))
            ).bounds(x + index * (width + gap), y, width, 20).build();
            button.active = index != this.section;
            addButton(button);
        }
    }

    private Button addButton(Button button) {
        this.styledButtons.add(button);
        return this.addRenderableWidget(button);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        QuestLedgerTheme.drawBackdrop(graphics, this.frame);
        QuestLedgerTheme.drawHeader(graphics, this.frame, this.contentTop - 3);
        QuestLedgerTheme.drawFooter(graphics, this.frame, this.contentBottom);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                this.title.getString(),
                this.frame.width() - 48
        );
        graphics.text(
                this.font,
                Component.literal(title),
                this.frame.left() + (this.frame.width() - this.font.width(title)) / 2,
                this.frame.top() + 16,
                QuestLedgerTheme.INK,
                false
        );

        drawSection(graphics);
        for (Button button : this.styledButtons) {
            QuestLedgerTheme.drawButton(graphics, this.font, button, mouseX, mouseY);
        }
    }

    private void drawSection(GuiGraphicsExtractor graphics) {
        String prefix;
        String titleKey;
        int count;
        int accent;
        switch (this.section) {
            case MANUAL -> {
                prefix = "screen.questledger.types.manual";
                titleKey = prefix + ".title";
                count = 4;
                accent = QuestLedgerTheme.RED;
            }
            case HYBRID -> {
                prefix = "screen.questledger.types.hybrid";
                titleKey = prefix + ".title";
                count = 2;
                accent = QuestLedgerTheme.BLUE;
            }
            default -> {
                prefix = "screen.questledger.types.auto";
                titleKey = prefix + ".title";
                count = 6;
                accent = QuestLedgerTheme.GREEN;
            }
        }

        int left = this.frame.left() + this.padding;
        int right = this.frame.right() - this.padding;
        Component sectionTitle = Component.translatable(titleKey);
        String fittedTitle = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                sectionTitle.getString(),
                right - left
        );
        graphics.text(
                this.font,
                Component.literal(fittedTitle),
                left + 4,
                this.contentTop + 4,
                accent,
                false
        );

        int gridTop = this.contentTop + (this.frame.tiny() ? 19 : 21);
        int availableHeight = Math.max(30, this.contentBottom - gridTop - 5);
        // Never collapse the compact guide to one column: six rows cannot fit its height.
        int columns = count > 1 ? 2 : 1;
        int rows = (count + columns - 1) / columns;
        int gap = this.frame.tiny() ? 4 : 6;
        int cardWidth = Math.max(40, (right - left - gap * (columns - 1)) / columns);
        int cardHeight = Math.max(
                16,
                Math.min(42, (availableHeight - gap * Math.max(0, rows - 1)) / rows)
        );

        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = left + column * (cardWidth + gap);
            int y = gridTop + row * (cardHeight + gap);
            int bottom = Math.min(this.contentBottom - 3, y + cardHeight);
            QuestLedgerTheme.drawCard(graphics, x, y, x + cardWidth, bottom, false);

            String number = Integer.toString(index + 1);
            int badgeWidth = 18;
            QuestLedgerTheme.drawBadge(
                    graphics,
                    this.font,
                    Component.literal(number),
                    x + 7,
                    y + Math.max(3, (cardHeight - 13) / 2),
                    badgeWidth,
                    0xFFE4C98E,
                    accent
            );

            String example = Component.translatable(prefix + "." + (index + 1)).getString();
            if (example.startsWith("• ")) {
                example = example.substring(2);
            }
            int textX = x + 32;
            int maximumWidth = Math.max(8, cardWidth - 39);
            String fitted = QuestLedgerUiLayout.ellipsize(
                    this.font::width,
                    example,
                    maximumWidth
            );
            graphics.text(
                    this.font,
                    Component.literal(fitted),
                    textX,
                    y + Math.max(5, (cardHeight - 9) / 2),
                    QuestLedgerTheme.MUTED,
                    false
            );
        }
    }

    private void show(Screen screen) {
        this.minecraft.gui.setScreen(screen);
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
