package dev.aether.questledger.questscript;

import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.LogicalOperator;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;

import java.util.stream.Collectors;

/**
 * User-facing QuestScript formatter. Timer fields are deliberately omitted.
 */
public final class QuestScriptUserFormatter {
    public String format(QuestFile file) {
        return file.quests().stream().map(this::formatQuest)
                .collect(Collectors.joining("\n\n")) + (file.quests().isEmpty() ? "" : "\n");
    }

    private String formatQuest(QuestDefinition quest) {
        StringBuilder out = new StringBuilder();
        out.append("quest ").append(quote(quest.title())).append(" {\n");
        quest.id().ifPresent(id -> out.append("  id ").append(quote(id)).append("\n"));
        quest.description().ifPresent(description -> out.append("  description ")
                .append(quote(description)).append("\n"));
        if (quest.id().isPresent() || quest.description().isPresent()) {
            out.append('\n');
        }
        out.append("  complete when {\n");
        out.append(formatExpression(quest.completionCondition(), 4, 0));
        out.append("\n  }\n");
        quest.animation().ifPresent(animation -> out.append("\n  animation ")
                .append(quote(animation)).append("\n"));
        if (!quest.hudVisible()) {
            out.append("  hud hide\n");
        }
        out.append('}');
        return out.toString();
    }

    private String formatExpression(Expression expression, int indent, int parentPrecedence) {
        int precedence = precedence(expression);
        String rendered = switch (expression) {
            case Expression.NumberLiteral number -> number.sourceText();
            case Expression.StringLiteral string -> quote(string.value());
            case Expression.BooleanLiteral bool -> Boolean.toString(bool.value());
            case Expression.Reference reference -> reference.qualifiedName();
            case Expression.Call call -> call.qualifiedName() + "(" + call.arguments().stream()
                    .map(argument -> formatExpression(argument, indent, 0))
                    .collect(Collectors.joining(", ")) + ")";
            case Expression.Not not -> "not " + formatExpression(not.operand(), indent, precedence);
            case Expression.Comparison comparison -> formatExpression(comparison.left(), indent, precedence)
                    + " " + comparison.operator().symbol() + " "
                    + formatExpression(comparison.right(), indent, precedence);
            case Expression.Logical logical -> {
                String operator = logical.operator() == LogicalOperator.AND ? "and" : "or";
                yield formatExpression(logical.left(), indent, precedence)
                        + "\n" + " ".repeat(indent) + operator + " "
                        + formatExpression(logical.right(), indent, precedence);
            }
        };
        return precedence < parentPrecedence ? "(" + rendered + ")" : rendered;
    }

    private static int precedence(Expression expression) {
        return switch (expression) {
            case Expression.Logical logical -> logical.operator() == LogicalOperator.OR ? 1 : 2;
            case Expression.Not ignored -> 4;
            case Expression.Comparison ignored -> 3;
            default -> 5;
        };
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
