package dev.aether.questledger.questscript;

public final class QuestScriptException extends RuntimeException {
    private final String code;
    private final SourceLocation location;

    public QuestScriptException(String code, SourceLocation location, String message) {
        super(message + " at " + location.line() + ":" + location.column());
        this.code = code;
        this.location = location;
    }

    public String code() {
        return code;
    }

    public SourceLocation location() {
        return location;
    }
}
