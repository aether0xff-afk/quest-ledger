package dev.aether.questledger.questscript;

import java.util.Objects;

public record Token(TokenType type, String lexeme, SourceLocation location) {
    public Token {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(lexeme, "lexeme");
        Objects.requireNonNull(location, "location");
    }
}
