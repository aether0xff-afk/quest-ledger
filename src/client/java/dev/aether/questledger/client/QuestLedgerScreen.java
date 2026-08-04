package dev.aether.questledger.client;

import dev.aether.questledger.questscript.QuestScript;
import dev.aether.questledger.questscript.QuestScriptException;
import dev.aether.questledger.questscript.QuestScriptUserFormatter;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class QuestLedgerScreen extends Screen {
    private enum Mode {
        BUILDER,
        CODE
    }

    private static final int PANEL_COLOR = 0xFFF0D9A4;
    private static final int PANEL_DARK = 0xFF5B3A24;
    private static final int PANEL_MID = 0xFF9A6A3B;
    private static final int INK = 0xFF2B1A12;
    private static final int MUTED_INK = 0xFF6F5139;
    private static final int ERROR_INK = 0xFF9D271E;
    private static final int SUCCESS_INK = 0xFF355B2A;

    private final Screen parent;
    private final QuestEditorModel model;
    private final Mode mode;
    private final Component status;
    private final boolean statusError;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

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
        this.panelWidth = Math.min(560, Math.max(300, this.width - 32));
        this.panelHeight = Math.min(350, Math.max(250, this.height - 32));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        addTabs();
        if (this.mode == Mode.BUILDER) {
            addBuilderWidgets();
        } else {
            addCodeEditor();
        }
        addFooterButtons();
    }

    private void addTabs() {
        int y = this.panelTop + 30;
        int width = (this.panelWidth - 54) / 2;

        Button builder = Button.builder(
                Component.translatable("screen.questledger.builder"),
                button -> switchToBuilder()
        ).bounds(this.panelLeft + 22, y, width, 20).build();
        builder.active = this.mode != Mode.BUILDER;
        this.addRenderableWidget(builder);

        Button code = Button.builder(
                Component.translatable("screen.questledger.code"),
                button -> switchToCode()
        ).bounds(this.panelLeft + 32 + width, y, width, 20).build();
        code.active = this.mode != Mode.CODE;
        this.addRenderableWidget(code);
    }

    private void addFooterButtons() {
        int y = this.panelTop + this.panelHeight - 32;
        int available = this.panelWidth - 52;
        int buttonWidth = Math.min(96, Math.max(70, (available - 16) / 3));

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.active"),
                button -> show(new QuestListScreen(this))
        ).bounds(this.panelLeft + 22, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.save"),
                button -> saveQuest()
        ).bounds(
                this.panelLeft + this.panelWidth - 22 - buttonWidth * 2 - 8,
                y,
                buttonWidth,
                20
        ).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.cancel"),
                button -> onClose()
        ).bounds(
                this.panelLeft + this.panelWidth - 22 - buttonWidth,
                y,
                buttonWidth,
                20
        ).build());
    }

    private void addBuilderWidgets() {
        int x = this.panelLeft + 154;
        int y = this.panelTop + 67;
        int width = this.panelWidth - 178;

        this.titleField = textField(x, y, width, "screen.questledger.field.title", this.model.title());

        y += 32;
        this.addRenderableWidget(Button.builder(conditionLabel(), button -> {
            syncBuilderFields();
            this.model.cycleConditionKind();
            button.setMessage(conditionLabel());
            updateConditionWidgets();
        }).bounds(x, y, width, 20).build());

        y += 32;
        this.targetField = textField(x, y, width, "screen.questledger.field.target", this.model.targetId());

        y += 32;
        this.operatorButton = this.addRenderableWidget(Button.builder(
                Component.literal(this.model.operator().symbol()),
                button -> {
                    this.model.cycleOperator();
                    button.setMessage(Component.literal(this.model.operator().symbol()));
                }
        ).bounds(x, y, 54, 20).build());
        this.minusButton = this.addRenderableWidget(Button.builder(
                Component.literal("−"),
                button -> adjustAmount(-1)
        ).bounds(x + 62, y, 28, 20).build());
        this.amountField = textField(
                x + 96,
                y,
                Math.max(54, width - 130),
                "screen.questledger.field.amount",
                Integer.toString(this.model.amount())
        );
        this.plusButton = this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                button -> adjustAmount(1)
        ).bounds(x + width - 28, y, 28, 20).build());

        y += 32;
        this.addRenderableWidget(Button.builder(animationLabel(), button -> {
            this.model.cycleAnimation();
            button.setMessage(animationLabel());
        }).bounds(x, y, width, 20).build());

        y += 32;
        this.addRenderableWidget(Button.builder(hudLabel(), button -> {
            this.model.toggleHudVisible();
            button.setMessage(hudLabel());
        }).bounds(x, y, width, 20).build());

        updateConditionWidgets();
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
                width,
                20,
                Component.translatable(narrationKey)
        );
        field.setValue(value);
        return this.addRenderableWidget(field);
    }

    private void addCodeEditor() {
        int x = this.panelLeft + 24;
        int y = this.panelTop + 67;
        this.codeEditor = MultiLineEditBox.builder()
                .setShowDecorations(false)
                .setShowBackground(false)
                .setTextColor(INK)
                .setCursorColor(PANEL_DARK)
                .setTextShadow(false)
                .setX(x)
                .setY(y)
                .build(
                        this.font,
                        this.panelWidth - 48,
                        this.panelHeight - 119,
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
                this.panelTop + 56,
                this.panelLeft + this.panelWidth - 14,
                this.panelTop + 58,
                PANEL_MID
        );

        if (this.mode == Mode.CODE) {
            graphics.fill(
                    this.panelLeft + 18,
                    this.panelTop + 61,
                    this.panelLeft + this.panelWidth - 18,
                    this.panelTop + this.panelHeight - 43,
                    0x553C2415
            );
            graphics.fill(
                    this.panelLeft + 21,
                    this.panelTop + 64,
                    this.panelLeft + this.panelWidth - 21,
                    this.panelTop + this.panelHeight - 46,
                    0xFFF8E8BC
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

        int titleWidth = this.font.width(this.title);
        graphics.text(
                this.font,
                this.title,
                this.panelLeft + (this.panelWidth - titleWidth) / 2,
                this.panelTop + 11,
                INK,
                false
        );

        if (this.mode == Mode.BUILDER) {
            drawBuilderLabels(graphics);
        }
        if (!this.status.getString().isBlank()) {
            graphics.text(
                    this.font,
                    this.status,
                    this.panelLeft + 24,
                    this.panelTop + this.panelHeight - 52,
                    this.statusError ? ERROR_INK : SUCCESS_INK,
                    false
            );
        }
    }

    private void drawBuilderLabels(GuiGraphicsExtractor graphics) {
        int x = this.panelLeft + 24;
        int y = this.panelTop + 73;
        String[] keys = {
                "screen.questledger.field.title",
                "screen.questledger.field.condition",
                "screen.questledger.field.target",
                "screen.questledger.field.amount",
                "screen.questledger.field.animation",
                "screen.questledger.field.hud"
        };
        for (int index = 0; index < keys.length; index++) {
            graphics.text(
                    this.font,
                    Component.translatable(keys[index]),
                    x,
                    y + index * 32,
                    INK,
                    false
            );
        }

        Component preview;
        if (this.model.conditionKind() == QuestEditorModel.ConditionKind.MANUAL) {
            preview = Component.translatable("screen.questledger.manual_hint");
        } else {
            preview = Component.literal(
                    this.model.conditionKind().functionName()
                            + "(\"" + this.model.targetId() + "\") "
                            + this.model.operator().symbol() + " " + this.model.amount()
            );
        }
        graphics.text(
                this.font,
                preview,
                this.panelLeft + 24,
                this.panelTop + this.panelHeight - 72,
                MUTED_INK,
                false
        );
    }

    @Override
    public void onClose() {
        show(this.parent);
    }
}
