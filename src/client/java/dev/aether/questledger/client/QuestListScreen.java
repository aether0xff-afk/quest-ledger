package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class QuestListScreen extends Screen {
    private static final int QUESTS_PER_PAGE = 6;
    private static final int PANEL_COLOR = 0xFFF0D9A4;
    private static final int PANEL_DARK = 0xFF5B3A24;
    private static final int PANEL_MID = 0xFF9A6A3B;
    private static final int INK = 0xFF2B1A12;
    private static final int MUTED_INK = 0xFF6F5139;

    private final Screen parent;
    private final int requestedPage;

    private int page;
    private int pageCount;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public QuestListScreen(Screen parent) {
        this(parent, 0);
    }

    private QuestListScreen(Screen parent, int page) {
        super(Component.translatable("screen.questledger.list.title"));
        this.parent = parent;
        this.requestedPage = page;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(600, Math.max(320, this.width - 32));
        this.panelHeight = Math.min(380, Math.max(260, this.height - 32));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        List<QuestDefinition> quests = ClientQuestStore.snapshot().quests();
        this.pageCount = Math.max(1, (quests.size() + QUESTS_PER_PAGE - 1) / QUESTS_PER_PAGE);
        this.page = Math.max(0, Math.min(this.requestedPage, this.pageCount - 1));

        int firstIndex = this.page * QUESTS_PER_PAGE;
        int lastIndex = Math.min(quests.size(), firstIndex + QUESTS_PER_PAGE);
        for (int index = firstIndex; index < lastIndex; index++) {
            addQuestButton(quests.get(index), index - firstIndex);
        }

        int footerY = this.panelTop + this.panelHeight - 31;
        this.addRenderableWidget(Button.builder(
                Component.literal("‹"),
                button -> show(new QuestListScreen(this.parent, this.page - 1))
        ).bounds(this.panelLeft + 20, footerY, 28, 20).build()).active = this.page > 0;

        this.addRenderableWidget(Button.builder(
                Component.literal("›"),
                button -> show(new QuestListScreen(this.parent, this.page + 1))
        ).bounds(this.panelLeft + 54, footerY, 28, 20).build()).active = this.page + 1 < this.pageCount;

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.types.button"),
                button -> show(new QuestTypesScreen(this))
        ).bounds(this.panelLeft + 94, footerY, 112, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.back"),
                button -> onClose()
        ).bounds(this.panelLeft + this.panelWidth - 112, footerY, 92, 20).build());
    }

    private void addQuestButton(QuestDefinition quest, int row) {
        int y = this.panelTop + 55 + row * 45;
        QuestExpressionInspector.CompletionMode mode = QuestExpressionInspector.completionMode(quest);
        Component label;
        boolean active;

        if (mode == QuestExpressionInspector.CompletionMode.AUTOMATIC) {
            label = Component.translatable("screen.questledger.list.automatic");
            active = false;
        } else if (ManualQuestStore.isChecked(quest)) {
            label = Component.translatable("screen.questledger.list.confirmed");
            active = false;
        } else {
            label = Component.translatable("screen.questledger.list.confirm");
            active = !QuestCompletionController.isCompleting(quest);
        }

        Button button = Button.builder(label, ignored -> {
            ManualQuestStore.markChecked(quest);
            show(new QuestListScreen(this.parent, this.page));
        }).bounds(this.panelLeft + this.panelWidth - 132, y + 7, 104, 20).build();
        button.active = active;
        this.addRenderableWidget(button);
    }

    private void show(Screen screen) {
        this.minecraft.gui.setScreen(screen);
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

        List<QuestDefinition> quests = ClientQuestStore.snapshot().quests();
        int firstIndex = this.page * QUESTS_PER_PAGE;
        int lastIndex = Math.min(quests.size(), firstIndex + QUESTS_PER_PAGE);
        if (quests.isEmpty()) {
            graphics.text(
                    this.font,
                    Component.translatable("screen.questledger.list.empty"),
                    this.panelLeft + 24,
                    this.panelTop + 64,
                    MUTED_INK,
                    false
            );
        }

        for (int index = firstIndex; index < lastIndex; index++) {
            drawQuest(graphics, quests.get(index), index - firstIndex);
        }

        Component pageLabel = Component.translatable(
                "screen.questledger.list.page",
                this.page + 1,
                this.pageCount
        );
        graphics.text(
                this.font,
                pageLabel,
                this.panelLeft + 218,
                this.panelTop + this.panelHeight - 25,
                MUTED_INK,
                false
        );
    }

    private void drawQuest(
            GuiGraphicsExtractor graphics,
            QuestDefinition quest,
            int row
    ) {
        int y = this.panelTop + 55 + row * 45;
        graphics.fill(
                this.panelLeft + 18,
                y,
                this.panelLeft + this.panelWidth - 18,
                y + 38,
                0x22FFFFFF
        );
        graphics.text(
                this.font,
                Component.literal(quest.title()),
                this.panelLeft + 28,
                y + 7,
                INK,
                false
        );

        String modeKey = switch (QuestExpressionInspector.completionMode(quest)) {
            case AUTOMATIC -> "automatic";
            case MANUAL -> "manual";
            case HYBRID -> "hybrid";
        };
        Component detail = Component.translatable(
                "screen.questledger.list.detail",
                Component.translatable("screen.questledger.mode." + modeKey),
                Component.translatable(
                        "screen.questledger.category."
                                + QuestExpressionInspector.categoryKey(quest)
                )
        );
        graphics.text(
                this.font,
                detail,
                this.panelLeft + 28,
                y + 22,
                MUTED_INK,
                false
        );
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
