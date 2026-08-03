package dev.aether.questledger.questscript.validation;

import java.util.List;

public record ValidationResult(List<Diagnostic> diagnostics) {
    public ValidationResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean valid() {
        return diagnostics.isEmpty();
    }
}
