package dev.aether.questledger.questscript;

import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.QuestFile;

public final class QuestScriptSelfTest {
    private static final String VALID_SOURCE = """
            quest "네더라이트 하나 더" {
              id "one_more_netherite"
              description "네더라이트 주괴를 하나 확보한다."

              complete when {
                inventory.count("minecraft:netherite_ingot") >= 1
                or (
                  player.dimension == "minecraft:the_nether"
                  and stat.mined("minecraft:ancient_debris") >= 4
                )
              }

              hold 1s
              remove after 1200ms
              animation "wax_seal"
            }
            """;

    public static void main(String[] args) {
        QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(VALID_SOURCE);
        require(parsed.validation().valid(), "Expected valid script: " + parsed.validation().diagnostics());
        require(parsed.file().quests().size() == 1, "Expected one quest");
        require(parsed.file().quests().getFirst().completionCondition() instanceof Expression.Logical,
                "Expected logical root expression");

        String persistenceFormatted = new QuestScriptFormatter().format(parsed.file());
        QuestFile reparsed = QuestScript.parse(persistenceFormatted);
        require(reparsed.quests().size() == 1, "Persistence source must parse");

        String userFormatted = new QuestScriptUserFormatter().format(parsed.file());
        require(!userFormatted.contains("hold "), "User source must hide legacy hold timers");
        require(!userFormatted.contains("remove after"),
                "User source must hide legacy removal timers");
        require(QuestScript.parse(userFormatted).quests().size() == 1,
                "Timer-free user source must parse");

        QuestScript.ParsedQuestScript manual = QuestScript.parseAndValidate("""
                quest "금 공장 완성" {
                  complete when {
                    manual.checked == true
                  }
                  animation "page_fold"
                }
                """);
        require(manual.validation().valid(), "Expected valid manual quest");

        QuestScript.ParsedQuestScript hybrid = QuestScript.parseAndValidate("""
                quest "금 공장 검증" {
                  complete when {
                    manual.checked == true
                    and inventory.count("minecraft:gold_ingot") >= 64
                  }
                }
                """);
        require(hybrid.validation().valid(), "Expected valid hybrid quest");

        QuestScript.ParsedQuestScript invalid = QuestScript.parseAndValidate("""
                quest "잘못된 퀘스트" {
                  complete when {
                    inventory.amount("diamond") >= 4
                  }
                  animation "unknown"
                }
                """);
        require(!invalid.validation().valid(), "Expected invalid script");
        require(invalid.validation().diagnostics().stream()
                        .anyMatch(diagnostic -> diagnostic.code().equals("UNKNOWN_FUNCTION")),
                "Expected UNKNOWN_FUNCTION diagnostic");
        require(invalid.validation().diagnostics().stream()
                        .anyMatch(diagnostic -> diagnostic.code().equals("UNSUPPORTED_ANIMATION")),
                "Expected UNSUPPORTED_ANIMATION diagnostic");

        System.out.println("QuestScript self-test passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
