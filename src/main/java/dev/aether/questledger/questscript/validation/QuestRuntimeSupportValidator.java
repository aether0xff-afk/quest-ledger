package dev.aether.questledger.questscript.validation;

import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.QuestFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Verifies that a semantically valid QuestScript file only uses expressions that the
 * Minecraft 26.2 client runtime can currently evaluate. The language registry is deliberately
 * broader than the current runtime so older and forward-looking files remain parseable; new
 * saves must not silently accept conditions that can never complete.
 */
public final class QuestRuntimeSupportValidator {
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(
            "player.health",
            "player.max_health",
            "player.hunger",
            "player.experience_level",
            "player.x",
            "player.y",
            "player.z",
            "player.dimension",
            "player.on_ground",
            "player.is_sneaking",
            "player.is_sprinting",
            "manual.checked"
    );

    private static final Set<String> SUPPORTED_FUNCTIONS = Set.of(
            "inventory.count",
            "inventory.has",
            "inventory.equipped",
            "inventory.durability",
            "stat.mined",
            "stat.used",
            "stat.crafted",
            "stat.killed",
            "stat.picked_up",
            "stat.dropped",
            "distance.to",
            "inside.box",
            "inside.radius",
            "quest.active"
    );

    private static final Set<String> EXACT_RESOURCE_FUNCTIONS = Set.of(
            "inventory.count",
            "inventory.has",
            "inventory.equipped",
            "inventory.durability",
            "stat.mined",
            "stat.used",
            "stat.crafted",
            "stat.killed",
            "stat.picked_up",
            "stat.dropped"
    );

    public ValidationResult validate(QuestFile file) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        file.quests().forEach(quest -> inspect(quest.completionCondition(), diagnostics));
        return new ValidationResult(diagnostics);
    }

    private void inspect(Expression expression, List<Diagnostic> diagnostics) {
        switch (expression) {
            case Expression.NumberLiteral ignored -> { }
            case Expression.StringLiteral ignored -> { }
            case Expression.BooleanLiteral ignored -> { }
            case Expression.Reference reference -> {
                if (!SUPPORTED_PROPERTIES.contains(reference.qualifiedName())) {
                    diagnostics.add(new Diagnostic(
                            "UNSUPPORTED_RUNTIME",
                            "Runtime property is not implemented yet: "
                                    + reference.qualifiedName(),
                            reference.location()
                    ));
                }
            }
            case Expression.Call call -> {
                if (!SUPPORTED_FUNCTIONS.contains(call.qualifiedName())) {
                    diagnostics.add(new Diagnostic(
                            "UNSUPPORTED_RUNTIME",
                            "Runtime function is not implemented yet: "
                                    + call.qualifiedName(),
                            call.location()
                    ));
                } else if (EXACT_RESOURCE_FUNCTIONS.contains(call.qualifiedName())
                        && !call.arguments().isEmpty()
                        && call.arguments().getFirst() instanceof Expression.StringLiteral target
                        && target.value().startsWith("#")) {
                    diagnostics.add(new Diagnostic(
                            "UNSUPPORTED_RUNTIME",
                            "Item and block tags are not supported at runtime yet; use an exact ID",
                            target.location()
                    ));
                }
                call.arguments().forEach(argument -> inspect(argument, diagnostics));
            }
            case Expression.Not not -> inspect(not.operand(), diagnostics);
            case Expression.Logical logical -> {
                inspect(logical.left(), diagnostics);
                inspect(logical.right(), diagnostics);
            }
            case Expression.Comparison comparison -> {
                inspect(comparison.left(), diagnostics);
                inspect(comparison.right(), diagnostics);
            }
        }
    }
}
