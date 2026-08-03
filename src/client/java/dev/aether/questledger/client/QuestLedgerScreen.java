package dev.aether.questledger.client;

import dev.aether.questledger.questscript.QuestScript;
import dev.aether.questledger.questscript.QuestScriptException;
import dev.aether.questledger.questscript.QuestScriptFormatter;
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

    private Button conditionButton;
    private Button operatorButton;
    private Button animationButton;
    private Button hudButton;

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

        int tabY = this.panelTop + 30;
        int halfTab = (this.panelWidth - 54) / 2;

        Button builderTab = Button.builder(
                Component.translatable("screen.questledger.builder"),
                button -> switchToBuilder()
        ).bounds(this.panelLeft + 22, tabY, halfTab, 20).build();
        builderTab.active = this.mode != Mode.BUILDER;
        this.addRenderableWidget(builderTab);

        Button codeTab = Button.builder(
                Component.translatable("screen.questledger.code"),
                button -> switchToCode()
        ).bounds(this.panelLeft + 32 + halfTab, tabY, halfTab, 20).build();
        codeTab.active = this.mode != Mode.CODE;
        this.addRenderableWidget(codeTab);

        if (this.mode == Mode.BUILDER) {
            initBuilderWidgets();
        } else {
            initCodeWidgets();
        }

        int footerY = this.panelTop + this.panelHeight - 32;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.save"),
                button -> saveQuest()
        ).bounds(this.panelLeft + this.panelWidth - 218, footerY, 96, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.questledger.cancel"),
                button -> onClose()
        ).bounds(this.panelLeft + this.panelWidth - 114, footerY, 92, 20).build());
    }

    private void initBuilderWidgets() {
        int x = this.panelLeft + 154;
        int y = this.panelTop + 67;
        int fieldWidth = this.panelWidth - 178;

        this.titleField = new EditBox(
                this.font, x, y, fieldWidth, 20,
                Component.translatable("screen.questledger.field.title")
        );
        this.titleField.setValue(this.model.title());
        this.addRenderableWidget(this.titleField);

        y += 32;
        this.conditionButton = this.addRenderableWidget(Button.builder(
                conditionLabel(),
                button -> {
                    syncBuilderFields();
                    this.model.cycleConditionKind();
                    button.setMessage(conditionLabel());
                }
        ).bounds(x, y, fieldWidth, 20).build());

        y += 32;
        this.targetField = new EditBox(
                this.font, x, y, fieldWidth, 20,
                Component.translatable("screen.questledger.field.target")
        );
        this.targetField.setValue(this.model.targetId());
        this.addRenderableWidget(this.targetField);

        y += 32;
        this.operatorButton = this.addRenderableWidget(Button.builder(
                Component.literal(this.model.operator().symbol()),
                button -> {
                    this.model.cycleOperator();
                    button.setMessage(Component.literal(this.model.operator().symbol()));
                }
        ).bounds(x, y, 54, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("−"),
                button -> adjustAmount(-1)
        ).bounds(x + 62, y, 28, 20).build());

        this.amountField = new EditBox(
                this.font, x + 96, y, Math.max(54, fieldWidth - 130), 20,
                Component.translatable("screen.questledger.field.amount")
        );
        this.amountField.setValue(Integer.toString(this.model.amount()));
        this.addRenderableWidget(this.amountField);

        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                button -> adjustAmount(1)
        ).bounds(x + fieldWidth - 28, y, 28, 20).build());

        y += 32;
        this.animationButton = this.addRenderableWidget(Button.builder(
                animationLabel(),
                button -> {
                    this.model.cycleAnimation();
                    button.setMessage(animationLabel());
                }
        ).bounds(x, y, fieldWidth, 20).build());

        y += 32;
        this.hudButton = this.addRenderableWidget(Button.builder(
                hudLabel(),
                button -> {
                    this.model.toggleHudVisible();
                    button.setMessage(hudLabel());
                }
        ).bounds(x, y, fieldWidth, 20).build());
    }

    private void initCodeWidgets() {
        int x = this.panelLeft + 24;
        int y = this.panelTop + 67;
        int editorWidth = this.panelWidth - 48;
        int editorHeight = this.panelHeight - 119;

        this.codeEditor = MultiLineEditBox.builder()
                .setShowDecorations(false)
                .setShowBackground(false)
                .setTextColor(INK)
                .setCursorColor(PANEL_DARK)
                .setTextShadow(false)
                .setX(x)
                .setY(y)
                .build(this.font, editorWidth, editorHeight,
                        Component.translatable("screen.questledger.code.placeholder"));
        this.codeEditor.setCharacterLimit(16_384);
        this.codeEditor.setLineLimit(128);
        this.codeEditor.setValue(this.model.codeSource());
        this.addRenderableWidget(this.codeEditor);
    }

    private void switchToCode() {
        syncBuilderFields();
        this.model.codeSource(this.model.toCanonicalSource());
        this.minecraft.setScreen(new QuestLedgerScreen(
                this.parent, this.model, Mode.CODE, Component.empty(), false
        ));
    }

    private void switchToBuilder() {
        if (this.codeEditor != null) {
            this.model.codeSource(this.codeEditor.value());
            try {
                QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(this.model.codeSource());
                if (!parsed.validation().valid()) {
                    var diagnostic = parsed.validation().diagnostics().getFirst();
                    reopenCode(Component.literal(diagnostic.code() + ": " + diagnostic.message()), true);
                    return;
                }
                if (parsed.file().quests().size() != 1) {
                    reopenCode(Component.translatable("screen.questledger.error.single_quest"), true);
                    return;
                }
                QuestDefinition quest = parsed.file().quests().getFirst();
                if (!this.model.importQuest(quest)) {
                    reopenCode(Component.translatable("screen.questledger.error.builder_subset"), true);
                    return;
                }
            } catch (QuestScriptException exception) {
                reopenCode(Component.literal(exception.getMessage()), true);
                return;
            }
        }

        this.minecraft.setScreen(new QuestLedgerScreen(
                this.parent, this.model, Mode.BUILDER, Component.empty(), false
        ));
    }

    private void reopenCode(Component message, boolean error) {
        this.minecraft.setScreen(new QuestLedgerScreen(
                this.parent, this.model, Mode.CODE, message, error
        ));
    }

    private void saveQuest() {
        ClientQuestStore.SaveResult result;
        if (this.mode == Mode.BUILDER) {
            syncBuilderFields();
            result = ClientQuestStore.append(this.model.toCanonicalSource());
        } else {
            this.model.codeSource(this.codeEditor.value());
            result = ClientQuestStore.append(this.model.codeSource());
            if (result.success()) {
                try {
                    var parsed = QuestScript.parse(this.model.codeSource());
                    this.model.codeSource(new QuestScriptFormatter().format(parsed));
                } catch (QuestScriptException ignored) {
                    // append already returned success, so this is defensive only.
                }
            }
        }

        this.minecraft.setScreen(new QuestLedgerScreen(
                this.parent,
                this.model,
                this.mode,
                Component.literal(result.message()),
                !result.success()
        ));
    }

    private void syncBuilderFields() {
        if (this.titleField != null) {
            this.model.title(this.titleField.getValue());
        }
        if (this.targetField != null) {
            this.model.targetId(this.targetField.getValue());
        }
        if (this.amountField != null) {
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
        return Component.translatable("screen.questledger.condition."
                + this.model.conditionKind().name().toLowerCase());
    }

    private Component animationLabel() {
        return Component.translatable(
                "screen.questledger.animation",
                Component.translatable("screen.questledger.animation."
                        + this.model.animation().name().toLowerCase())
        );
    }

    private Component hudLabel() {
        return Component.translatable(
                "screen.questledger.hud",
                Component.translatable(this.model.hudVisible()
                        ? "screen.questledger.on"
                        : "screen.questledger.off")
        );
    }

    @Override
    protected void extractBackground(
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
            int x = this.panelLeft + 24;
            int y = this.panelTop + 73;
            graphics.text(this.font, Component.translatable("screen.questledger.field.title"),
                    x, y, INK, false);
            graphics.text(this.font, Component.translatable("screen.questledger.field.condition"),
                    x, y + 32, INK, false);
            graphics.text(this.font, Component.translatable("screen.questledger.field.target"),
                    x, y + 64, INK, false);
            graphics.text(this.font, Component.translatable("screen.questledger.field.amount"),
                    x, y + 96, INK, false);
            graphics.text(this.font, Component.translatable("screen.questledger.field.animation"),
                    x, y + 128, INK, false);
            graphics.text(this.font, Component.translatable("screen.questledger.field.hud"),
                    x, y + 160, INK, false);

            String preview = this.model.conditionKind().functionName()
                    + "(\"" + this.model.targetId() + "\") "
                    + this.model.operator().symbol() + " " + this.model.amount();
            graphics.text(this.font, preview,
                    this.panelLeft + 24,
                    this.panelTop + this.panelHeight - 54,
                    MUTED_INK,
                    false);
        }

        if (!this.status.getString().isBlank()) {
            graphics.text(
                    this.font,
                    this.status,
                    this.panelLeft + 24,
                    this.panelTop + this.panelHeight - 27,
                    this.statusError ? ERROR_INK : SUCCESS_INK,
                    false
            );
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
