package dev.aether.questledger.questscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Lexer {
    private static final Map<String, TokenType> KEYWORDS = createKeywords();

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start;
    private int current;
    private int line = 1;
    private int column = 1;
    private int tokenLine = 1;
    private int tokenColumn = 1;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            tokenLine = line;
            tokenColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", new SourceLocation(line, column, current)));
        return List.copyOf(tokens);
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '{' -> add(TokenType.LEFT_BRACE);
            case '}' -> add(TokenType.RIGHT_BRACE);
            case '(' -> add(TokenType.LEFT_PAREN);
            case ')' -> add(TokenType.RIGHT_PAREN);
            case ',' -> add(TokenType.COMMA);
            case '.' -> add(TokenType.DOT);
            case '=' -> {
                if (match('=')) add(TokenType.EQUAL_EQUAL);
                else error("SYNTAX_ERROR", "Expected '=' after '='");
            }
            case '!' -> {
                if (match('=')) add(TokenType.BANG_EQUAL);
                else error("SYNTAX_ERROR", "Expected '=' after '!'");
            }
            case '>' -> add(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '<' -> add(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case ' ', '\r', '\t' -> { }
            case '\n' -> { }
            case '#' -> skipLineComment();
            case '/' -> {
                if (match('/')) skipLineComment();
                else error("SYNTAX_ERROR", "Unexpected '/'");
            }
            case '"' -> string();
            default -> {
                if (isDigit(c) || (c == '-' && isDigit(peek()))) number();
                else if (isIdentifierStart(c)) identifier();
                else error("SYNTAX_ERROR", "Unexpected character '" + c + "'");
            }
        }
    }

    private void string() {
        StringBuilder value = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\\') {
                if (isAtEnd()) error("UNTERMINATED_STRING", "Unterminated escape sequence");
                char escaped = advance();
                switch (escaped) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> error("INVALID_ESCAPE", "Unsupported escape sequence \\" + escaped + "'");
                }
            } else {
                value.append(c);
            }
        }
        if (isAtEnd()) error("UNTERMINATED_STRING", "Unterminated string");
        advance();
        tokens.add(new Token(TokenType.STRING, value.toString(), location()));
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        add(TokenType.NUMBER);
    }

    private void identifier() {
        while (isIdentifierPart(peek())) advance();
        String text = source.substring(start, current);
        add(KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER));
    }

    private void skipLineComment() {
        while (!isAtEnd() && peek() != '\n') advance();
    }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        advance();
        return true;
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private char peekNext() {
        return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void add(TokenType type) {
        tokens.add(new Token(type, source.substring(start, current), location()));
    }

    private SourceLocation location() {
        return new SourceLocation(tokenLine, tokenColumn, start);
    }

    private void error(String code, String message) {
        throw new QuestScriptException(code, location(), message);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static Map<String, TokenType> createKeywords() {
        Map<String, TokenType> map = new HashMap<>();
        map.put("quest", TokenType.QUEST);
        map.put("id", TokenType.ID);
        map.put("description", TokenType.DESCRIPTION);
        map.put("complete", TokenType.COMPLETE);
        map.put("when", TokenType.WHEN);
        map.put("hold", TokenType.HOLD);
        map.put("remove", TokenType.REMOVE);
        map.put("after", TokenType.AFTER);
        map.put("animation", TokenType.ANIMATION);
        map.put("hud", TokenType.HUD);
        map.put("show", TokenType.SHOW);
        map.put("hide", TokenType.HIDE);
        map.put("and", TokenType.AND);
        map.put("or", TokenType.OR);
        map.put("not", TokenType.NOT);
        map.put("true", TokenType.TRUE);
        map.put("false", TokenType.FALSE);
        return Map.copyOf(map);
    }
}
