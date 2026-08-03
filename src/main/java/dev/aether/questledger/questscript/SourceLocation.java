package dev.aether.questledger.questscript;

public record SourceLocation(int line, int column, int offset) {
    public SourceLocation {
        if (line < 1 || column < 1 || offset < 0) {
            throw new IllegalArgumentException("Invalid source location");
        }
    }
}
