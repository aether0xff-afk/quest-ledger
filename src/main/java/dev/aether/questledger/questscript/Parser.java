package dev.aether.questledger.questscript;

import dev.aether.questledger.questscript.ast.ComparisonOperator;
import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.LogicalOperator;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Parser {
    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    public QuestFile parseFile() {
        List<QuestDefinition> quests = new ArrayList<>();
        while (!isAtEnd()) {
            quests.add(parseQuest());
        }
        return new QuestFile(quests);
    }

    private QuestDefinition parseQuest() {
        consume(TokenType.QUEST, "Expected 'quest'");
        String title = consume(TokenType.STRING, "Expected quest title string").lexeme();
        consume(TokenType.LEFT_BRACE, "Expected '{' after quest title");

        Optional<String> id = Optional.empty();
        Optional<String> description = Optional.empty();
        Expression completion = null;
        Duration hold = Duration.ZERO;
        Duration removeAfter = Duration.ofMillis(1200);
        Optional<String> animation = Optional.empty();
        boolean hudVisible = true;

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.ID)) {
                id = Optional.of(consume(TokenType.STRING, "Expected ID string").lexeme());
            } else if (match(TokenType.DESCRIPTION)) {
                description = Optional.of(consume(TokenType.STRING, "Expected description string").lexeme());
            } else if (match(TokenType.COMPLETE)) {
                consume(TokenType.WHEN, "Expected 'when' after 'complete'");
                consume(TokenType.LEFT_BRACE, "Expected '{' before completion condition");
                completion = expression();
                consume(TokenType.RIGHT_BRACE, "Expected '}' after completion condition");
            } else if (match(TokenType.HOLD)) {
                hold = duration();
            } else if (match(TokenType.REMOVE)) {
                consume(TokenType.AFTER, "Expected 'after' after 'remove'");
                removeAfter = duration();
            } else if (match(TokenType.ANIMATION)) {
                animation = Optional.of(consume(TokenType.STRING, "Expected animation ID string").lexeme());
            } else if (match(TokenType.HUD)) {
                if (match(TokenType.SHOW)) hudVisible = true;
                else if (match(TokenType.HIDE)) hudVisible = false;
                else throw error(peek(), "Expected 'show' or 'hide' after 'hud'");
            } else {
                throw error(peek(), "Unknown quest field '" + peek().lexeme() + "'");
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after quest");
        if (completion == null) {
            throw error(previous(), "Quest requires exactly one 'complete when' field");
        }

        return new QuestDefinition(title, id, description, completion, hold,
                removeAfter, animation, hudVisible);
    }

    private Duration duration() {
        Token number = consume(TokenType.NUMBER, "Expected duration number");
        Token unit = consume(TokenType.IDENTIFIER, "Expected duration unit: ms, s, m, or h");
        double value;
        try {
            value = Double.parseDouble(number.lexeme());
        } catch (NumberFormatException exception) {
            throw error(number, "Invalid duration number");
        }
        if (!Double.isFinite(value) || value < 0) {
            throw error(number, "Duration must be a finite non-negative number");
        }
        long millis = switch (unit.lexeme()) {
            case "ms" -> Math.round(value);
            case "s" -> Math.round(value * 1_000d);
            case "m" -> Math.round(value * 60_000d);
            case "h" -> Math.round(value * 3_600_000d);
            default -> throw error(unit, "Unsupported duration unit '" + unit.lexeme() + "'");
        };
        return Duration.ofMillis(millis);
    }

    private Expression expression() {
        return or();
    }

    private Expression or() {
        Expression expression = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            expression = new Expression.Logical(expression, LogicalOperator.OR, and(), operator.location());
        }
        return expression;
    }

    private Expression and() {
        Expression expression = unary();
        while (match(TokenType.AND)) {
            Token operator = previous();
            expression = new Expression.Logical(expression, LogicalOperator.AND, unary(), operator.location());
        }
        return expression;
    }

    private Expression unary() {
        if (match(TokenType.NOT)) {
            Token operator = previous();
            return new Expression.Not(unary(), operator.location());
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expression nested = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return nested;
        }
        return comparison();
    }

    private Expression comparison() {
        Expression left = primary();
        if (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL, TokenType.GREATER,
                TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expression right = primary();
            return new Expression.Comparison(left, comparisonOperator(operator.type()), right, operator.location());
        }
        return left;
    }

    private Expression primary() {
        if (match(TokenType.TRUE)) return new Expression.BooleanLiteral(true, previous().location());
        if (match(TokenType.FALSE)) return new Expression.BooleanLiteral(false, previous().location());
        if (match(TokenType.NUMBER)) {
            Token token = previous();
            try {
                return new Expression.NumberLiteral(Double.parseDouble(token.lexeme()), token.lexeme(), token.location());
            } catch (NumberFormatException exception) {
                throw error(token, "Invalid number");
            }
        }
        if (match(TokenType.STRING)) {
            Token token = previous();
            return new Expression.StringLiteral(token.lexeme(), token.location());
        }
        if (match(TokenType.IDENTIFIER, TokenType.QUEST)) {
            Token first = previous();
            List<String> path = new ArrayList<>();
            path.add(first.lexeme());
            while (match(TokenType.DOT)) {
                path.add(consume(TokenType.IDENTIFIER, "Expected name after '.'").lexeme());
            }
            if (match(TokenType.LEFT_PAREN)) {
                List<Expression> arguments = new ArrayList<>();
                if (!check(TokenType.RIGHT_PAREN)) {
                    do {
                        arguments.add(expression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
                return new Expression.Call(path, arguments, first.location());
            }
            return new Expression.Reference(path, first.location());
        }
        throw error(peek(), "Expected expression");
    }

    private static ComparisonOperator comparisonOperator(TokenType type) {
        return switch (type) {
            case EQUAL_EQUAL -> ComparisonOperator.EQUAL;
            case BANG_EQUAL -> ComparisonOperator.NOT_EQUAL;
            case GREATER -> ComparisonOperator.GREATER;
            case GREATER_EQUAL -> ComparisonOperator.GREATER_EQUAL;
            case LESS -> ComparisonOperator.LESS;
            case LESS_EQUAL -> ComparisonOperator.LESS_EQUAL;
            default -> throw new IllegalArgumentException("Not a comparison operator: " + type);
        };
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(Math.max(0, current - 1));
    }

    private QuestScriptException error(Token token, String message) {
        return new QuestScriptException("SYNTAX_ERROR", token.location(), message);
    }
}
