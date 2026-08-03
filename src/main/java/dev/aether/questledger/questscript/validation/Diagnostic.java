package dev.aether.questledger.questscript.validation;

import dev.aether.questledger.questscript.SourceLocation;

import java.util.Objects;

public record Diagnostic(String code, String message, SourceLocation location) {
    public Diagnostic {
        Objects.requireNonNull(code);
        Objects.requireNonNull(message);
        Objects.requireNonNull(location);
    }
}
