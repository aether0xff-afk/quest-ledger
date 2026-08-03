package dev.aether.questledger.client;

import dev.aether.questledger.questscript.QuestScriptFormatter;
import dev.aether.questledger.questscript.SourceLocation;
import dev.aether.questledger.questscript.ast.ComparisonOperator;
import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class QuestEditorModel {
    public enum ConditionKind {
        INVENTORY("inventory.count"),
        BLOCK_MINED("stat.mined"),
        MOB_KILLED("stat.killed");

        private final String functionName;

        ConditionKind(String functionName) {
            this.functionName = functionName;
        }

        public String functionName() {
            return this.functionName;
        }

        public ConditionKind next() {
            ConditionKind[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Optional<ConditionKind> fromFunction(String name) {
            for (ConditionKind value : values()) {
                if (value.functionName.equals(name)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    public enum Operator {
        AT_LEAST(">=", ComparisonOperator.GREATER_EQUAL),
        EQUAL("==", ComparisonOperator.EQUAL),
        AT_MOST("<=", ComparisonOperator.LESS_EQUAL);

        private final String symbol;
        private final ComparisonOperator astOperator;

        Operator(String symbol, ComparisonOperator astOperator) {
            this.symbol = symbol;
            this.astOperator = astOperator;
        }

        public String symbol() {
            return this.symbol;
        }

        public Operator next() {
            Operator[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Optional<Operator> fromAst(ComparisonOperator operator) {
            for (Operator value : values()) {
                if (value.astOperator == operator) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    public enum Animation {
        WAX_SEAL("wax_seal"),
        INK_CHECK("ink_check"),
        PAGE_FOLD("page_fold");

        private final String id;

        Animation(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }

        public Animation next() {
            Animation[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Animation fromId(String id) {
            for (Animation animation : values()) {
                if (animation.id.equals(id)) {
                    return animation;
                }
            }
            return WAX_SEAL;
        }
    }

    private String title = "새 퀘스트";
    private String targetId = "minecraft:diamond";
    private int amount = 1;
    private ConditionKind conditionKind = ConditionKind.INVENTORY;
    private Operator operator = Operator.AT_LEAST;
    private Animation animation = Animation.WAX_SEAL;
    private boolean hudVisible = true;
    private String codeSource = "";

    public String title() {
        return title;
    }

    public void title(String title) {
        this.title = title == null || title.isBlank() ? "새 퀘스트" : title.strip();
    }

    public String targetId() {
        return targetId;
    }

    public void targetId(String targetId) {
        this.targetId = targetId == null || targetId.isBlank()
                ? "minecraft:diamond" : targetId.strip().toLowerCase(Locale.ROOT);
    }

    public int amount() {
        return amount;
    }

    public void amount(int amount) {
        this.amount = Math.max(0, Math.min(1_000_000, amount));
    }

    public ConditionKind conditionKind() {
        return conditionKind;
    }

    public void cycleConditionKind() {
        this.conditionKind = this.conditionKind.next();
    }

    public Operator operator() {
        return operator;
    }

    public void cycleOperator() {
        this.operator = this.operator.next();
    }

    public Animation animation() {
        return animation;
    }

    public void cycleAnimation() {
        this.animation = this.animation.next();
    }

    public boolean hudVisible() {
        return hudVisible;
    }

    public void toggleHudVisible() {
        this.hudVisible = !this.hudVisible;
    }

    public String codeSource() {
        if (this.codeSource.isBlank()) {
            this.codeSource = toCanonicalSource();
        }
        return this.codeSource;
    }

    public void codeSource(String source) {
        this.codeSource = source == null ? "" : source;
    }

    public String toCanonicalSource() {
        QuestDefinition quest = toQuestDefinition();
        return new QuestScriptFormatter().format(new QuestFile(List.of(quest)));
    }

    public QuestDefinition toQuestDefinition() {
        SourceLocation location = new SourceLocation(1, 1, 0);
        Expression call = new Expression.Call(
                List.of(this.conditionKind.functionName().split("\\.")),
                List.of(new Expression.StringLiteral(this.targetId, location)),
                location
        );
        Expression number = new Expression.NumberLiteral(
                this.amount,
                Integer.toString(this.amount),
                location
        );
        Expression condition = new Expression.Comparison(
                call,
                this.operator.astOperator,
                number,
                location
        );

        return new QuestDefinition(
                this.title,
                Optional.empty(),
                Optional.empty(),
                condition,
                Duration.ZERO,
                Duration.ofMillis(1200),
                Optional.of(this.animation.id),
                this.hudVisible
        );
    }

    public boolean importQuest(QuestDefinition quest) {
        if (!(quest.completionCondition() instanceof Expression.Comparison comparison)) {
            return false;
        }
        if (!(comparison.left() instanceof Expression.Call call)
                || call.arguments().size() != 1
                || !(call.arguments().getFirst() instanceof Expression.StringLiteral target)
                || !(comparison.right() instanceof Expression.NumberLiteral number)) {
            return false;
        }

        Optional<ConditionKind> importedKind = ConditionKind.fromFunction(call.qualifiedName());
        Optional<Operator> importedOperator = Operator.fromAst(comparison.operator());
        if (importedKind.isEmpty() || importedOperator.isEmpty()) {
            return false;
        }

        this.title = quest.title();
        this.targetId = target.value();
        this.amount = (int) Math.max(0, Math.min(1_000_000, Math.round(number.value())));
        this.conditionKind = importedKind.get();
        this.operator = importedOperator.get();
        this.animation = quest.animation().map(Animation::fromId).orElse(Animation.WAX_SEAL);
        this.hudVisible = quest.hudVisible();
        this.codeSource = new QuestScriptFormatter().format(new QuestFile(List.of(quest)));
        return true;
    }
}
