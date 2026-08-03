package dev.aether.questledger.questscript;

import dev.aether.questledger.questscript.ast.QuestFile;
import dev.aether.questledger.questscript.validation.QuestScriptValidator;
import dev.aether.questledger.questscript.validation.ValidationResult;

public final class QuestScript {
    private QuestScript() { }

    public static QuestFile parse(String source) {
        return new Parser(new Lexer(source).scanTokens()).parseFile();
    }

    public static ValidationResult validate(QuestFile file) {
        return new QuestScriptValidator().validate(file);
    }

    public static ParsedQuestScript parseAndValidate(String source) {
        QuestFile file = parse(source);
        return new ParsedQuestScript(file, validate(file));
    }

    public record ParsedQuestScript(QuestFile file, ValidationResult validation) { }
}
