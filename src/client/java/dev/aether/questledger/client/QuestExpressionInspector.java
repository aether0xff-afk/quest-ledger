package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.QuestDefinition;

public final class QuestExpressionInspector {
    public enum CompletionMode {
        AUTOMATIC,
        MANUAL,
        HYBRID
    }

    private QuestExpressionInspector() {
    }

    public static CompletionMode completionMode(QuestDefinition quest) {
        boolean manual = containsManual(quest.completionCondition());
        if (!manual) {
            return CompletionMode.AUTOMATIC;
        }
        return containsAutomaticSource(quest.completionCondition())
                ? CompletionMode.HYBRID
                : CompletionMode.MANUAL;
    }

    public static boolean containsManual(Expression expression) {
        return switch (expression) {
            case Expression.Reference reference -> reference.qualifiedName().equals("manual.checked");
            case Expression.Call ignored -> false;
            case Expression.Not not -> containsManual(not.operand());
            case Expression.Comparison comparison -> containsManual(comparison.left())
                    || containsManual(comparison.right());
            case Expression.Logical logical -> containsManual(logical.left())
                    || containsManual(logical.right());
            default -> false;
        };
    }

    public static String categoryKey(QuestDefinition quest) {
        Expression expression = quest.completionCondition();
        if (containsFunction(expression, "stat.killed")) {
            return "combat";
        }
        if (containsFunction(expression, "stat.mined")) {
            return "mining";
        }
        if (containsFunction(expression, "stat.crafted")
                || containsFunction(expression, "stat.used")) {
            return "crafting";
        }
        if (containsFunction(expression, "inventory.count")
                || containsFunction(expression, "inventory.has")
                || containsFunction(expression, "inventory.equipped")
                || containsFunction(expression, "inventory.durability")) {
            return "inventory";
        }
        if (containsFunction(expression, "distance.to")
                || containsFunction(expression, "inside.box")
                || containsFunction(expression, "inside.radius")
                || containsReferencePrefix(expression, "player.x")
                || containsReferencePrefix(expression, "player.y")
                || containsReferencePrefix(expression, "player.z")
                || containsReferencePrefix(expression, "player.dimension")) {
            return "location";
        }
        if (containsManual(expression)) {
            return "build";
        }
        return "other";
    }

    private static boolean containsAutomaticSource(Expression expression) {
        return switch (expression) {
            case Expression.Reference reference -> !reference.qualifiedName().equals("manual.checked");
            case Expression.Call ignored -> true;
            case Expression.Not not -> containsAutomaticSource(not.operand());
            case Expression.Comparison comparison -> containsAutomaticSource(comparison.left())
                    || containsAutomaticSource(comparison.right());
            case Expression.Logical logical -> containsAutomaticSource(logical.left())
                    || containsAutomaticSource(logical.right());
            default -> false;
        };
    }

    private static boolean containsFunction(Expression expression, String qualifiedName) {
        return switch (expression) {
            case Expression.Call call -> call.qualifiedName().equals(qualifiedName)
                    || call.arguments().stream().anyMatch(argument ->
                    containsFunction(argument, qualifiedName));
            case Expression.Not not -> containsFunction(not.operand(), qualifiedName);
            case Expression.Comparison comparison -> containsFunction(
                    comparison.left(), qualifiedName
            ) || containsFunction(comparison.right(), qualifiedName);
            case Expression.Logical logical -> containsFunction(logical.left(), qualifiedName)
                    || containsFunction(logical.right(), qualifiedName);
            default -> false;
        };
    }

    private static boolean containsReferencePrefix(Expression expression, String prefix) {
        return switch (expression) {
            case Expression.Reference reference -> reference.qualifiedName().startsWith(prefix);
            case Expression.Call call -> call.arguments().stream().anyMatch(argument ->
                    containsReferencePrefix(argument, prefix));
            case Expression.Not not -> containsReferencePrefix(not.operand(), prefix);
            case Expression.Comparison comparison -> containsReferencePrefix(
                    comparison.left(), prefix
            ) || containsReferencePrefix(comparison.right(), prefix);
            case Expression.Logical logical -> containsReferencePrefix(logical.left(), prefix)
                    || containsReferencePrefix(logical.right(), prefix);
            default -> false;
        };
    }
}
