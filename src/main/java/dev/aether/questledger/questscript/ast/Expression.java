package dev.aether.questledger.questscript.ast;

import dev.aether.questledger.questscript.SourceLocation;

import java.util.List;
import java.util.Objects;

public sealed interface Expression permits Expression.Logical, Expression.Not,
        Expression.Comparison, Expression.NumberLiteral, Expression.StringLiteral,
        Expression.BooleanLiteral, Expression.Reference, Expression.Call {

    SourceLocation location();

    record Logical(Expression left, LogicalOperator operator, Expression right,
                   SourceLocation location) implements Expression {
        public Logical {
            Objects.requireNonNull(left);
            Objects.requireNonNull(operator);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record Not(Expression operand, SourceLocation location) implements Expression {
        public Not {
            Objects.requireNonNull(operand);
            Objects.requireNonNull(location);
        }
    }

    record Comparison(Expression left, ComparisonOperator operator, Expression right,
                      SourceLocation location) implements Expression {
        public Comparison {
            Objects.requireNonNull(left);
            Objects.requireNonNull(operator);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record NumberLiteral(double value, String sourceText,
                         SourceLocation location) implements Expression {
        public NumberLiteral {
            Objects.requireNonNull(sourceText);
            Objects.requireNonNull(location);
        }
    }

    record StringLiteral(String value, SourceLocation location) implements Expression {
        public StringLiteral {
            Objects.requireNonNull(value);
            Objects.requireNonNull(location);
        }
    }

    record BooleanLiteral(boolean value, SourceLocation location) implements Expression {
        public BooleanLiteral {
            Objects.requireNonNull(location);
        }
    }

    record Reference(List<String> path, SourceLocation location) implements Expression {
        public Reference {
            path = List.copyOf(path);
            if (path.isEmpty()) throw new IllegalArgumentException("Reference path cannot be empty");
            Objects.requireNonNull(location);
        }

        public String qualifiedName() {
            return String.join(".", path);
        }
    }

    record Call(List<String> path, List<Expression> arguments,
                SourceLocation location) implements Expression {
        public Call {
            path = List.copyOf(path);
            arguments = List.copyOf(arguments);
            if (path.isEmpty()) throw new IllegalArgumentException("Call path cannot be empty");
            Objects.requireNonNull(location);
        }

        public String qualifiedName() {
            return String.join(".", path);
        }
    }
}
