package dev.aether.questledger.questscript.validation;

import dev.aether.questledger.questscript.SourceLocation;
import dev.aether.questledger.questscript.ast.ComparisonOperator;
import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;
import dev.aether.questledger.questscript.ast.ValueType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class QuestScriptValidator {
    private static final Pattern QUEST_ID = Pattern.compile("[a-z][a-z0-9_]{0,63}");
    private static final Pattern RESOURCE_ID = Pattern.compile("#?[a-z0-9_.-]+:[a-z0-9_./-]+");

    public ValidationResult validate(QuestFile file) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Set<String> questIds = new HashSet<>();

        for (QuestDefinition quest : file.quests()) {
            quest.id().ifPresent(id -> {
                if (!QUEST_ID.matcher(id).matches()) {
                    diagnostics.add(new Diagnostic("INVALID_QUEST_ID",
                            "Quest ID must match [a-z][a-z0-9_]{0,63}",
                            quest.completionCondition().location()));
                } else if (!questIds.add(id)) {
                    diagnostics.add(new Diagnostic("DUPLICATE_QUEST_ID",
                            "Duplicate quest ID '" + id + "'",
                            quest.completionCondition().location()));
                }
            });

            ValueType type = infer(quest.completionCondition(), diagnostics);
            if (type != ValueType.BOOLEAN && type != ValueType.UNKNOWN) {
                diagnostics.add(new Diagnostic("TYPE_MISMATCH",
                        "Completion condition must evaluate to boolean, but was " + type,
                        quest.completionCondition().location()));
            }

            quest.animation().ifPresent(animation -> {
                if (!QuestScriptRegistry.ANIMATIONS.contains(animation)) {
                    diagnostics.add(new Diagnostic("UNSUPPORTED_ANIMATION",
                            "Unknown animation '" + animation + "'",
                            quest.completionCondition().location()));
                }
            });
        }

        return new ValidationResult(diagnostics);
    }

    private ValueType infer(Expression expression, List<Diagnostic> diagnostics) {
        return switch (expression) {
            case Expression.NumberLiteral ignored -> ValueType.NUMBER;
            case Expression.StringLiteral ignored -> ValueType.STRING;
            case Expression.BooleanLiteral ignored -> ValueType.BOOLEAN;
            case Expression.Reference reference -> inferReference(reference, diagnostics);
            case Expression.Call call -> inferCall(call, diagnostics);
            case Expression.Not not -> {
                ValueType operand = infer(not.operand(), diagnostics);
                requireType(operand, ValueType.BOOLEAN, not.location(), diagnostics,
                        "'not' requires a boolean operand");
                yield ValueType.BOOLEAN;
            }
            case Expression.Logical logical -> {
                ValueType left = infer(logical.left(), diagnostics);
                ValueType right = infer(logical.right(), diagnostics);
                requireType(left, ValueType.BOOLEAN, logical.left().location(), diagnostics,
                        "Logical operator requires boolean operands");
                requireType(right, ValueType.BOOLEAN, logical.right().location(), diagnostics,
                        "Logical operator requires boolean operands");
                yield ValueType.BOOLEAN;
            }
            case Expression.Comparison comparison -> inferComparison(comparison, diagnostics);
        };
    }

    private ValueType inferReference(Expression.Reference reference, List<Diagnostic> diagnostics) {
        ValueType type = QuestScriptRegistry.PROPERTIES.get(reference.qualifiedName());
        if (type == null) {
            diagnostics.add(new Diagnostic("UNKNOWN_PROPERTY",
                    "Unknown property '" + reference.qualifiedName() + "'", reference.location()));
            return ValueType.UNKNOWN;
        }
        return type;
    }

    private ValueType inferCall(Expression.Call call, List<Diagnostic> diagnostics) {
        FunctionSignature signature = QuestScriptRegistry.FUNCTIONS.get(call.qualifiedName());
        if (signature == null) {
            diagnostics.add(new Diagnostic("UNKNOWN_FUNCTION",
                    "Unknown function '" + call.qualifiedName() + "'", call.location()));
            for (Expression argument : call.arguments()) infer(argument, diagnostics);
            return ValueType.UNKNOWN;
        }

        if (call.arguments().size() != signature.argumentTypes().size()) {
            diagnostics.add(new Diagnostic("ARGUMENT_COUNT",
                    "Function '" + call.qualifiedName() + "' expects "
                            + signature.argumentTypes().size() + " arguments but received "
                            + call.arguments().size(), call.location()));
        }

        int common = Math.min(call.arguments().size(), signature.argumentTypes().size());
        for (int index = 0; index < call.arguments().size(); index++) {
            ValueType actual = infer(call.arguments().get(index), diagnostics);
            if (index < common) {
                requireType(actual, signature.argumentTypes().get(index),
                        call.arguments().get(index).location(), diagnostics,
                        "Invalid argument " + (index + 1) + " for '" + call.qualifiedName() + "'");
            }
        }

        if (signature.resourceIdFirstArgument() && !call.arguments().isEmpty()
                && call.arguments().getFirst() instanceof Expression.StringLiteral stringLiteral
                && !RESOURCE_ID.matcher(stringLiteral.value()).matches()) {
            diagnostics.add(new Diagnostic("INVALID_RESOURCE_ID",
                    "Expected a namespaced Minecraft resource ID, for example minecraft:diamond",
                    stringLiteral.location()));
        }

        return signature.returnType();
    }

    private ValueType inferComparison(Expression.Comparison comparison,
                                      List<Diagnostic> diagnostics) {
        ValueType left = infer(comparison.left(), diagnostics);
        ValueType right = infer(comparison.right(), diagnostics);

        if (left != ValueType.UNKNOWN && right != ValueType.UNKNOWN && left != right) {
            diagnostics.add(new Diagnostic("TYPE_MISMATCH",
                    "Cannot compare " + left + " with " + right,
                    comparison.location()));
        }

        boolean equality = comparison.operator() == ComparisonOperator.EQUAL
                || comparison.operator() == ComparisonOperator.NOT_EQUAL;
        if (!equality && left != ValueType.NUMBER && left != ValueType.UNKNOWN) {
            diagnostics.add(new Diagnostic("INVALID_OPERATOR",
                    "Operator '" + comparison.operator().symbol() + "' requires numbers",
                    comparison.location()));
        }
        return ValueType.BOOLEAN;
    }

    private static void requireType(ValueType actual, ValueType expected,
                                    SourceLocation location, List<Diagnostic> diagnostics,
                                    String message) {
        if (actual != ValueType.UNKNOWN && actual != expected) {
            diagnostics.add(new Diagnostic("TYPE_MISMATCH",
                    message + ": expected " + expected + " but was " + actual, location));
        }
    }
}
