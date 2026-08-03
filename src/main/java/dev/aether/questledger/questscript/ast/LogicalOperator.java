package dev.aether.questledger.questscript.ast;

public enum LogicalOperator {
    AND("and"),
    OR("or");

    private final String keyword;

    LogicalOperator(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
