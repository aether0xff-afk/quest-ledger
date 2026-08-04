package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class QuestListScreen extends Screen {
    private final Screen parent;
    private final int requestedPage;
    private final List<QuestDefinition> previewQuests;
    private final List<Button> styledButtons = new ArrayList<>();

    private QuestLedgerUiLayout.Frame frame;
    private QuestLedgerUiLayout.ListLayout layout;
    private int page;
    private int pageCount;
    private int actionWidth;
    private int pageLabelLeft;
    private int pageLabelWidth;

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
        this.styledButtons.clear();
        this.frame = QuestLedgerUiLayout.frame(this.width, this.height);
        this.layout = QuestLedgerUiLayout.list(this.frame);
        this.actionWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.list.confirm").getString(),
                this.frame.tiny() ? 68 : 84,
                this.frame.tiny() ? 92 : 126
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
        int gap = this.frame.tiny() ? 4 : 5;
        int arrowWidth = this.frame.tiny() ? 24 : 28;

        Button previous = addButton(Button.builder(
                Component.literal("‹"),
                button -> show(new QuestListScreen(this.parent, this.page - 1, this.previewQuests))
        ).bounds(x, y, arrowWidth, 20).build());
        previous.active = this.page > 0;

        int nextX = x + arrowWidth + gap;
        Button next = addButton(Button.builder(
                Component.literal("›"),
                button -> show(new QuestListScreen(this.parent, this.page + 1, this.previewQuests))
        ).bounds(nextX, y, arrowWidth, 20).build());
        next.active = this.page + 1 < this.pageCount;

        int backWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.back").getString(),
                this.frame.tiny() ? 52 : 68,
                this.frame.tiny() ? 72 : 100
        );
        int typesWidth = QuestLedgerUiLayout.buttonWidth(
                this.font::width,
                Component.translatable("screen.questledger.types.button").getString(),
                this.frame.tiny() ? 68 : 84,
                this.frame.tiny() ? 88 : 126
        );
        int right = this.frame.right() - this.layout.padding();
        int typesX = right - backWidth - gap - typesWidth;

        addButton(Button.builder(
                Component.translatable("screen.questledger.types.button"),
                button -> show(new QuestTypesScreen(this))
        ).bounds(typesX, y, typesWidth, 20).build());

        addButton(Button.builder(
                Component.translatable("screen.questledger.back"),
                button -> onClose()
        ).bounds(right - backWidth, y, backWidth, 20).build());

        int arrowsRight = nextX + arrowWidth;
        this.pageLabelLeft = arrowsRight + gap;
        this.pageLabelWidth = Math.max(0, typesX - gap - this.pageLabelLeft);
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
        addButton(button);
    }

    private Button addButton(Button button) {
        this.styledButtons.add(button);
        return this.addRenderableWidget(button);
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

        List<QuestDefinition> quests = quests();
        int firstIndex = this.page * this.layout.questsPerPage();
        int lastIndex = Math.min(quests.size(), firstIndex + this.layout.questsPerPage());
        if (quests.isEmpty()) {
            drawEmptyCard(graphics);
        } else {
            for (int index = firstIndex; index < lastIndex; index++) {
                drawQuestCard(graphics, quests.get(index), index - firstIndex);
            }
        }
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
            drawEmptyText(graphics);
        }

        for (int index = firstIndex; index < lastIndex; index++) {
            drawQuestContent(graphics, quests.get(index), index - firstIndex);
        }

        Component pageLabel = Component.translatable(
                "screen.questledger.list.page",
                this.page + 1,
                this.pageCount
        );
        int maximumPageWidth = this.frame.tiny()
                ? this.pageLabelWidth
                : Math.max(44, this.frame.width() / 4);
        String fittedPage = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                pageLabel.getString(),
                maximumPageWidth
        );
        int pageX = this.frame.tiny()
                ? this.pageLabelLeft + Math.max(0, (this.pageLabelWidth - this.font.width(fittedPage)) / 2)
                : this.frame.left() + (this.frame.width() - this.font.width(fittedPage)) / 2;
        graphics.text(
                this.font,
                Component.literal(fittedPage),
                pageX,
                this.frame.bottom() - 25,
                QuestLedgerTheme.MUTED,
                false
        );

        for (Button button : this.styledButtons) {
            QuestLedgerTheme.drawButton(graphics, this.font, button, mouseX, mouseY);
        }
    }

    private int emptyCardLeft() {
        return this.frame.left() + this.layout.padding();
    }

    private int emptyCardRight() {
        return this.frame.right() - this.layout.padding();
    }

    private int emptyCardTop() {
        return this.layout.contentTop() + 14;
    }

    private int emptyCardBottom() {
        return Math.min(this.layout.contentBottom() - 10, emptyCardTop() + 72);
    }

    private void drawEmptyCard(GuiGraphicsExtractor graphics) {
        QuestLedgerTheme.drawCard(
                graphics,
                emptyCardLeft(),
                emptyCardTop(),
                emptyCardRight(),
                emptyCardBottom(),
                false
        );
    }

    private void drawEmptyText(GuiGraphicsExtractor graphics) {
        Component empty = Component.translatable("screen.questledger.list.empty");
        String fitted = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                empty.getString(),
                emptyCardRight() - emptyCardLeft() - 32
        );
        graphics.text(
                this.font,
                Component.literal(fitted),
                emptyCardLeft() + (emptyCardRight() - emptyCardLeft() - this.font.width(fitted)) / 2,
                emptyCardTop() + Math.max(12, (emptyCardBottom() - emptyCardTop() - 9) / 2),
                QuestLedgerTheme.MUTED,
                false
        );
    }

    private void drawQuestCard(
            GuiGraphicsExtractor graphics,
            QuestDefinition quest,
            int row
    ) {
        int y = rowTop(row);
        int left = this.frame.left() + this.layout.padding();
        int right = this.frame.right() - this.layout.padding();
        int bottom = Math.min(this.layout.contentBottom(), y + this.layout.cardHeight());
        QuestLedgerTheme.drawCard(
                graphics,
                left,
                y,
                right,
                bottom,
                QuestCompletionController.isCompleting(quest)
        );
    }

    private void drawQuestContent(
            GuiGraphicsExtractor graphics,
            QuestDefinition quest,
            int row
    ) {
        int y = rowTop(row);
        int left = this.frame.left() + this.layout.padding();
        int right = this.frame.right() - this.layout.padding();
        int bottom = Math.min(this.layout.contentBottom(), y + this.layout.cardHeight());

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
        int badgeWidth = this.frame.tiny() ? 40 : 46;
        int badgeX = left + 10;
        int badgeY = y + Math.max(4, (this.layout.cardHeight() - 30) / 2);
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

        int textX = badgeX + badgeWidth + (this.frame.tiny() ? 6 : 9);
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
                badgeY - 1,
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
                Math.min(bottom - 11, badgeY + 15),
                QuestLedgerTheme.MUTED,
                false
        );
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
