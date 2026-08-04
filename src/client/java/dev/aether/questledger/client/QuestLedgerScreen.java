package dev.aether.questledger.client;

import dev.aether.questledger.questscript.QuestScript;
import dev.aether.questledger.questscript.QuestScriptException;
import dev.aether.questledger.questscript.QuestScriptUserFormatter;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.ui.QuestLedgerUiLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class QuestLedgerScreen extends Screen {
    private enum Mode {
        BUILDER,
        CODE
    }

    private final Screen parent;
    private final QuestEditorModel model;
    private final Mode mode;
    private final Component status;
    private final boolean statusError;
    private final List<Button> styledButtons = new ArrayList<>();

    private QuestLedgerUiLayout.Frame frame;
    private QuestLedgerUiLayout.Editor layout;

    private EditBox titleField;
    private EditBox targetField;
    private EditBox amountField;
    private MultiLineEditBox codeEditor;
    private Button operatorButton;
    private Button minusButton;
    private Button plusButton;

    public QuestLedgerScreen(Screen parent) {
        this(parent, new QuestEditorModel(), Mode.BUILDER, Component.empty(), false);
    }

    private QuestLedgerScreen(
            Screen parent,
            QuestEditorModel model,
            Mode mode,
            Component status,
            boolean statusError
    ) {
        super(Component.translatable("screen.questledger.title"));
        this.parent = parent;
        this.model = model;
        this.mode = mode;
        this.status = status;
        this.statusError = statusError;
    }

    @Override
    protected void init() {
        this.styledButtons.clear();
        this.frame = QuestLedgerUiLayout.frame(this.width, this.height);
        this.layout = QuestLedgerUiLayout.editor(this.frame);

        addTabs();
        if (this.mode == Mode.BUILDER) {
            addBuilderWidgets();
        } else {
            addCodeEditor();
        }
        addFooterButtons();
    }

    private void addTabs() {
        int gap = 6;
        int available = this.frame.width() - this.layout.padding() * 2;
        int width = Math.max(70, (available - gap) / 2);
        int x = this.frame.left() + this.layout.padding();
        int y = this.frame.top() + 33;

        Button builder = addButton(Button.builder(
                Component.translatable("screen.questledger.builder"),
                button -> switchToBuilder()
        ).bounds(x, y, width, 20).build());
        builder.active = this.mode != Mode.BUILDER;

        Button code = addButton(Button.builder(
                Component.translatable("screen.questledger.code"),
                button -> switchToCode()
        ).bounds(x + width + gap, y, width, 20).build());
        code.active = this.mode != Mode.CODE;
    }

    private void addFooterButtons() {
        int gap = 6;
        int available = this.frame.width() - this.layout.padding() * 2;
        int buttonWidth = Math.max(64, (available - gap * 2) / 3);
        int x = this.frame.left() + this.layout.padding();
        int y = this.frame.bottom() - 31;

        addButton(Button.builder(
                Component.translatable("screen.questledger.active"),
                button -> show(new QuestListScreen(this))
        ).bounds(x, y, buttonWidth, 20).build());

        addButton(Button.builder(
                Component.translatable("screen.questledger.save"),
                button -> saveQuest()
        ).bounds(x + buttonWidth + gap, y, buttonWidth, 20).build());

        addButton(Button.builder(
                Component.translatable("screen.questledger.cancel"),
                button -> onClose()
        ).bounds(x + (buttonWidth + gap) * 2, y, buttonWidth, 20).build());
    }

    private void addBuilderWidgets() {
        int x = this.layout.fieldLeft();
        int y = this.layout.contentTop();
        int width = this.layout.fieldWidth();
        int step = this.layout.rowStep();

        this.titleField = textField(x, y, width, "screen.questledger.field.title", this.model.title());

        y += step;
        addButton(Button.builder(conditionLabel(), button -> {
            syncBuilderFields();
            this.model.cycleConditionKind();
            button.setMessage(conditionLabel());
            updateConditionWidgets();
        }).bounds(x, y, width, 20).build());

        y += step;
        this.targetField = textField(x, y, width, "screen.questledger.field.target", this.model.targetId());

        y += step;
        addAmountWidgets(x, y, width);

        y += step;
        if (this.frame.tiny()) {
            int gap = 6;
            int half = Math.max(38, (width - gap) / 2);
            addAnimationButton(x, y, half);
            addHudButton(x + half + gap, y, Math.max(38, width - half - gap));
        } else {
            addAnimationButton(x, y, width);
            y += step;
            addHudButton(x, y, width);
        }

        updateConditionWidgets();
    }

    private void addAnimationButton(int x, int y, int width) {
        addButton(Button.builder(animationLabel(), button -> {
            this.model.cycleAnimation();
            button.setMessage(animationLabel());
        }).bounds(x, y, width, 20).build());
    }

    private void addHudButton(int x, int y, int width) {
        addButton(Button.builder(hudLabel(), button -> {
            this.model.toggleHudVisible();
            button.setMessage(hudLabel());
        }).bounds(x, y, width, 20).build());
    }

    private void addAmountWidgets(int x, int y, int width) {
        int gap = 4;
        int operatorWidth = Math.min(52, Math.max(38, width / 4));
        this.operatorButton = addButton(Button.builder(
                Component.literal(this.model.operator().symbol()),
                button -> {
                    this.model.cycleOperator();
                    button.setMessage(Component.literal(this.model.operator().symbol()));
                }
        ).bounds(x, y, operatorWidth, 20).build());

        if (width >= 154) {
            int small = 24;
            int amountWidth = Math.max(38, width - operatorWidth - small * 2 - gap * 3);
            this.minusButton = addButton(Button.builder(
                    Component.literal("−"),
                    button -> adjustAmount(-1)
            ).bounds(x + operatorWidth + gap, y, small, 20).build());
            this.amountField = textField(
                    x + operatorWidth + small + gap * 2,
                    y,
                    amountWidth,
                    "screen.questledger.field.amount",
                    Integer.toString(this.model.amount())
            );
            this.plusButton = addButton(Button.builder(
                    Component.literal("+"),
                    button -> adjustAmount(1)
            ).bounds(x + width - small, y, small, 20).build());
        } else {
            this.amountField = textField(
                    x + operatorWidth + gap,
                    y,
                    Math.max(32, width - operatorWidth - gap),
                    "screen.questledger.field.amount",
                    Integer.toString(this.model.amount())
            );
        }
    }

    private void updateConditionWidgets() {
        boolean enabled = this.model.conditionKind().requiresTarget();
        if (this.targetField != null) {
            this.targetField.active = enabled;
        }
        if (this.amountField != null) {
            this.amountField.active = enabled;
        }
        if (this.operatorButton != null) {
            this.operatorButton.active = enabled;
        }
        if (this.minusButton != null) {
            this.minusButton.active = enabled;
        }
        if (this.plusButton != null) {
            this.plusButton.active = enabled;
        }
    }

    private EditBox textField(int x, int y, int width, String narrationKey, String value) {
        EditBox field = new EditBox(
                this.font,
                x,
                y,
                Math.max(24, width),
                20,
                Component.translatable(narrationKey)
        );
        field.setValue(value);
        return this.addRenderableWidget(field);
    }

    private Button addButton(Button button) {
        this.styledButtons.add(button);
        return this.addRenderableWidget(button);
    }

    private void addCodeEditor() {
        int x = this.frame.left() + this.layout.padding();
        int y = this.layout.contentTop() + 4;
        int editorWidth = this.frame.width() - this.layout.padding() * 2;
        int editorHeight = Math.max(60, this.layout.contentBottom() - y - 4);
        this.codeEditor = MultiLineEditBox.builder()
                .setShowDecorations(false)
                .setShowBackground(false)
                .setTextColor(QuestLedgerTheme.INK)
                .setCursorColor(QuestLedgerTheme.LEATHER)
                .setTextShadow(false)
                .setX(x + 5)
                .setY(y + 5)
                .build(
                        this.font,
                        Math.max(40, editorWidth - 10),
                        Math.max(40, editorHeight - 10),
                        Component.translatable("screen.questledger.code.placeholder")
                );
        this.codeEditor.setCharacterLimit(16_384);
        this.codeEditor.setLineLimit(128);
        this.codeEditor.setValue(this.model.codeSource());
        this.addRenderableWidget(this.codeEditor);
    }

    private void switchToCode() {
        syncBuilderFields();
        this.model.codeSource(this.model.toCanonicalSource());
        show(new QuestLedgerScreen(this.parent, this.model, Mode.CODE, Component.empty(), false));
    }

    private void switchToBuilder() {
        if (this.codeEditor != null) {
            this.model.codeSource(this.codeEditor.getValue());
            try {
                QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(this.model.codeSource());
                if (!parsed.validation().valid()) {
                    var diagnostic = parsed.validation().diagnostics().getFirst();
                    reopenCode(Component.literal(
                            diagnostic.code() + ": " + diagnostic.message()
                    ), true);
                    return;
                }
                if (parsed.file().quests().size() != 1) {
                    reopenCode(Component.translatable(
                            "screen.questledger.error.single_quest"
                    ), true);
                    return;
                }
                QuestDefinition quest = parsed.file().quests().getFirst();
                if (!this.model.importQuest(quest)) {
                    reopenCode(Component.translatable(
                            "screen.questledger.error.builder_subset"
                    ), true);
                    return;
                }
            } catch (QuestScriptException exception) {
                reopenCode(Component.literal(exception.getMessage()), true);
                return;
            }
        }

        show(new QuestLedgerScreen(this.parent, this.model, Mode.BUILDER, Component.empty(), false));
    }

    private void reopenCode(Component message, boolean error) {
        show(new QuestLedgerScreen(this.parent, this.model, Mode.CODE, message, error));
    }

    private void saveQuest() {
        ClientQuestStore.SaveResult result;
        if (this.mode == Mode.BUILDER) {
            syncBuilderFields();
            result = ClientQuestStore.append(this.model.toCanonicalSource());
        } else {
            this.model.codeSource(this.codeEditor.getValue());
            result = ClientQuestStore.append(this.model.codeSource());
            if (result.success()) {
                try {
                    this.model.codeSource(new QuestScriptUserFormatter().format(
                            QuestScript.parse(this.model.codeSource())
                    ));
                } catch (QuestScriptException ignored) {
                    // append already validated this source.
                }
            }
        }

        show(new QuestLedgerScreen(
                this.parent,
                this.model,
                this.mode,
                Component.literal(result.message()),
                !result.success()
        ));
    }

    private void show(Screen screen) {
        this.minecraft.gui.setScreen(screen);
    }

    private void syncBuilderFields() {
        if (this.titleField != null) {
            this.model.title(this.titleField.getValue());
        }
        if (this.targetField != null && this.model.conditionKind().requiresTarget()) {
            this.model.targetId(this.targetField.getValue());
        }
        if (this.amountField != null && this.model.conditionKind().requiresTarget()) {
            try {
                this.model.amount(Integer.parseInt(this.amountField.getValue().strip()));
            } catch (NumberFormatException ignored) {
                this.model.amount(1);
            }
        }
    }

    private void adjustAmount(int delta) {
        syncBuilderFields();
        this.model.amount(this.model.amount() + delta);
        this.amountField.setValue(Integer.toString(this.model.amount()));
    }

    private Component conditionLabel() {
        return Component.translatable(
                "screen.questledger.condition."
                        + this.model.conditionKind().name().toLowerCase()
        );
    }

    private Component animationLabel() {
        return Component.translatable(
                "screen.questledger.animation",
                Component.translatable(
                        "screen.questledger.animation."
                                + this.model.animation().name().toLowerCase()
                )
        );
    }

    private Component hudLabel() {
        return Component.translatable(
                "screen.questledger.hud",
                Component.translatable(
                        this.model.hudVisible()
                                ? "screen.questledger.on"
                                : "screen.questledger.off"
                )
        );
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

        if (this.mode == Mode.CODE) {
            int x = this.frame.left() + this.layout.padding();
            int y = this.layout.contentTop() + 4;
            QuestLedgerTheme.drawInset(
                    graphics,
                    x,
                    y,
                    this.frame.right() - this.layout.padding(),
                    this.layout.contentBottom() - 4
            );
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

        String fittedTitle = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                this.title.getString(),
                this.frame.width() - 48
        );
        graphics.text(
                this.font,
                Component.literal(fittedTitle),
                this.frame.left() + (this.frame.width() - this.font.width(fittedTitle)) / 2,
                this.frame.top() + 14,
                QuestLedgerTheme.INK,
                false
        );

        if (this.mode == Mode.BUILDER) {
            drawBuilderLabels(graphics);
        }
        drawStatusOrPreview(graphics);
        for (Button button : this.styledButtons) {
            QuestLedgerTheme.drawButton(graphics, this.font, button, mouseX, mouseY);
        }
    }

    private void drawBuilderLabels(GuiGraphicsExtractor graphics) {
        int x = this.frame.left() + this.layout.padding();
        int y = this.layout.contentTop() + 6;
        String[] keys = this.frame.tiny()
                ? new String[] {
                        "screen.questledger.field.title",
                        "screen.questledger.field.condition",
                        "screen.questledger.field.target",
                        "screen.questledger.field.amount",
                        "screen.questledger.field.options"
                }
                : new String[] {
                        "screen.questledger.field.title",
                        "screen.questledger.field.condition",
                        "screen.questledger.field.target",
                        "screen.questledger.field.amount",
                        "screen.questledger.field.animation",
                        "screen.questledger.field.hud"
                };
        for (int index = 0; index < keys.length; index++) {
            QuestLedgerTheme.drawSectionLabel(
                    graphics,
                    this.font,
                    Component.translatable(keys[index]),
                    x,
                    y + index * this.layout.rowStep(),
                    this.layout.labelWidth(),
                    QuestLedgerTheme.INK
            );
        }
    }

    private void drawStatusOrPreview(GuiGraphicsExtractor graphics) {
        Component message;
        int color;
        if (!this.status.getString().isBlank()) {
            message = this.status;
            color = this.statusError ? QuestLedgerTheme.RED : QuestLedgerTheme.GREEN;
        } else if (this.mode == Mode.BUILDER) {
            if (this.model.conditionKind() == QuestEditorModel.ConditionKind.MANUAL) {
                message = Component.translatable("screen.questledger.manual_hint");
            } else {
                message = Component.literal(
                        this.model.conditionKind().functionName()
                                + "(\"" + this.model.targetId() + "\") "
                                + this.model.operator().symbol() + " " + this.model.amount()
                );
            }
            color = QuestLedgerTheme.MUTED;
        } else {
            message = Component.translatable("screen.questledger.code.hint");
            color = QuestLedgerTheme.MUTED;
        }

        int maximumWidth = this.frame.width() - this.layout.padding() * 2;
        String fitted = QuestLedgerUiLayout.ellipsize(
                this.font::width,
                message.getString(),
                maximumWidth
        );
        graphics.text(
                this.font,
                Component.literal(fitted),
                this.frame.left() + this.layout.padding(),
                this.layout.contentBottom() - 13,
                color,
                false
        );
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
