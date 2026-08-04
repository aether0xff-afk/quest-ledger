package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class QuestListScreen extends Screen {
    private final Screen parent;
    private final int requestedPage;
    private final List<QuestDefinition> previewQuests;

    private QuestLedgerUiLayout.Frame frame;
    private QuestLedgerUiLayout.ListLayout layout;
    private int page;
    private int pageCount;
    private int actionWidth;

    public QuestListScreen(Screen parent) {
        this(parent, 0, null);
    }

    private QuestListScreen(Screen parent, int page, List<QuestDefinition> previewQuests) {
        super(Component.translatable("screen.questledger.list.title"));
        this.parent = parent;
        this.requestedPage = page;
        this.previewQuests = previewQuests;
    }

    static QuestListScreen preview(Screen parent, List<QuestDefinition> quests) {
        return new QuestListScreen(parent, 0, List.copyOf(quests));
    }

    @Override
    protected void init() {
        this.frame = QuestLedgerUiLayout.frame(this.width, this.height);
        this.layout = QuestLedgerUiLayout.list(this.frame);
        this.actionWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.list.confirm").getString(),
                84,
                126
        );

        List<QuestDefinition> quests = quests();
        this.pageCount = Math.max(
                1,
                (quests.size() + this.layout.questsPerPage() - 1) / this.layout.questsPerPage()
        );
        this.page = Math.max(0, Math.min(this.requestedPage, this.pageCount - 1));

        int firstIndex = this.page * this.layout.questsPerPage();
        int lastIndex = Math.min(quests.size(), firstIndex + this.layout.questsPerPage());
        for (int index = firstIndex; index < lastIndex; index++) {
            addQuestButton(quests.get(index), index - firstIndex);
        }

        addFooterButtons();
    }

    private void addFooterButtons() {
        int y = this.frame.bottom() - 31;
        int x = this.frame.left() + this.layout.padding();
        int gap = 5;

        Button previous = this.addRenderableWidget(Button.builder(
                Component.literal("‹"),
                button -> show(new QuestListScreen(this.parent, this.page - 1, this.previewQuests))
        ).bounds(x, y, 28, 20).build());
        previous.active = this.page > 0;

        Button next = this.addRenderableWidget(Button.builder(
                Component.literal("›"),
                button -> show(new QuestListScreen(this.parent, this.page + 1, this.previewQuests))
        ).bounds(x + 28 + gap, y, 28, 20).build());
        next.active = this.page + 1 < this.pageCount;

        int backWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.back").getString(),
                68,
                100
        );
        int typesWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.types.button").getString(),
                84,
                126
        );
        int right = this.frame.right() - this.layout.padding();

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.types.button"),
                button -> show(new QuestTypesScreen(this))
        ).bounds(right - backWidth - gap - typesWidth, y, typesWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.back"),
                button -> onClose()
        ).bounds(right - backWidth, y, backWidth, 20).build());
    }

    private void addQuestButton(QuestDefinition quest, int row) {
        int y = rowTop(row);
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

        int right = this.frame.right() - this.layout.padding() - 8;
        Button button = Button.builder(label, ignored -> {
            ManualQuestStore.markChecked(quest);
            show(new QuestListScreen(this.parent, this.page, this.previewQuests));
        }).bounds(
                right - this.actionWidth,
                y + Math.max(2, (this.layout.cardHeight() - 20) / 2),
                this.actionWidth,
                20
        ).build();
        button.active = active;
        this.addRenderableWidget(button);
    }

    private int rowTop(int row) {
        return this.layout.contentTop()
                + row * (this.layout.cardHeight() + this.layout.gap());
    }

    private List<QuestDefinition> quests() {
        return this.previewQuests == null
                ? ClientQuestStore.snapshot().quests()
                : this.previewQuests;
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
        QuestLedgerTheme.drawBackdrop(graphics, this.frame);
        QuestLedgerTheme.drawHeader(graphics, this.frame, this.layout.contentTop() - 3);
        QuestLedgerTheme.drawFooter(graphics, this.frame, this.layout.contentBottom());
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
                this.frame.top() + 17,
                QuestLedgerTheme.INK,
                false
        );

        List<QuestDefinition> quests = quests();
        int firstIndex = this.page * this.layout.questsPerPage();
        int lastIndex = Math.min(quests.size(), firstIndex + this.layout.questsPerPage());
        if (quests.isEmpty()) {
            drawEmptyState(graphics);
        }

        for (int index = firstIndex; index < lastIndex; index++) {
            drawQuest(graphics, quests.get(index), index - firstIndex);
        }

        Component pageLabel = Component.translatable(
                "screen.questledger.list.page",
                this.page + 1,
                this.pageCount
        );
        String fittedPage = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                pageLabel.getString(),
                Math.max(44, this.frame.width() / 4)
        );
        graphics.text(
                this.font,
                Component.literal(fittedPage),
                this.frame.left() + (this.frame.width() - this.font.width(fittedPage)) / 2,
                this.frame.bottom() - 25,
                QuestLedgerTheme.MUTED,
                false
        );
    }

    private void drawEmptyState(GuiGraphicsExtractor graphics) {
        int cardLeft = this.frame.left() + this.layout.padding();
        int cardRight = this.frame.right() - this.layout.padding();
        int cardTop = this.layout.contentTop() + 14;
        int cardBottom = Math.min(this.layout.contentBottom() - 10, cardTop + 72);
        QuestLedgerTheme.drawCard(graphics, cardLeft, cardTop, cardRight, cardBottom, false);
        Component empty = Component.translatable("screen.questledger.list.empty");
        String fitted = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                empty.getString(),
                cardRight - cardLeft - 32
        );
        graphics.text(
                this.font,
                Component.literal(fitted),
                cardLeft + (cardRight - cardLeft - this.font.width(fitted)) / 2,
                cardTop + Math.max(12, (cardBottom - cardTop - 9) / 2),
                QuestLedgerTheme.MUTED,
                false
        );
    }

    private void drawQuest(
            GuiGraphicsExtractor graphics,
            QuestDefinition quest,
            int row
    ) {
        int y = rowTop(row);
        int left = this.frame.left() + this.layout.padding();
        int right = this.frame.right() - this.layout.padding();
        int bottom = Math.min(this.layout.contentBottom(), y + this.layout.cardHeight());
        boolean completing = QuestCompletionController.isCompleting(quest);
        QuestLedgerTheme.drawCard(graphics, left, y, right, bottom, completing);

        QuestExpressionInspector.CompletionMode completionMode =
                QuestExpressionInspector.completionMode(quest);
        String modeKey = switch (completionMode) {
            case AUTOMATIC -> "automatic";
            case MANUAL -> "manual";
            case HYBRID -> "hybrid";
        };
        int badgeColor = switch (completionMode) {
            case AUTOMATIC -> 0xFFD4E5D2;
            case MANUAL -> 0xFFE8D0C9;
            case HYBRID -> 0xFFD5DFE8;
        };
        int badgeInk = switch (completionMode) {
            case AUTOMATIC -> QuestLedgerTheme.GREEN;
            case MANUAL -> QuestLedgerTheme.RED;
            case HYBRID -> QuestLedgerTheme.BLUE;
        };
        int badgeWidth = 46;
        int badgeX = left + 10;
        int badgeY = y + 7;
        QuestLedgerTheme.drawBadge(
                graphics,
                this.font,
                Component.translatable("screen.questledger.mode." + modeKey),
                badgeX,
                badgeY,
                badgeWidth,
                badgeColor,
                badgeInk
        );

        int textX = badgeX + badgeWidth + 9;
        int actionLeft = right - 8 - this.actionWidth;
        int textWidth = Math.max(20, actionLeft - textX - 8);
        String fittedTitle = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                quest.title(),
                textWidth
        );
        graphics.text(
                this.font,
                Component.literal(fittedTitle),
                textX,
                y + 6,
                QuestLedgerTheme.INK,
                false
        );

        Component detail = Component.translatable(
                "screen.questledger.list.detail",
                Component.translatable("screen.questledger.mode." + modeKey),
                Component.translatable(
                        "screen.questledger.category."
                                + QuestExpressionInspector.categoryKey(quest)
                )
        );
        String fittedDetail = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                detail.getString(),
                textWidth
        );
        graphics.text(
                this.font,
                Component.literal(fittedDetail),
                textX,
                Math.min(bottom - 12, y + 22),
                QuestLedgerTheme.MUTED,
                false
        );
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
