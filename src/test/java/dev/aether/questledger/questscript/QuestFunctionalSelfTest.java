package dev.aether.questledger.questscript;

import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;
import dev.aether.questledger.questscript.validation.QuestRuntimeSupportValidator;
import dev.aether.questledger.questscript.validation.QuestScriptValidator;

import java.util.List;
import java.util.Random;

/** Dependency-free end-to-end checks for the save-facing QuestScript feature surface. */
public final class QuestFunctionalSelfTest {
    private static int checks;

    public static void main(String[] args) {
        validatesBuilderEquivalentMatrix();
        rejectsDuplicateIdsAcrossAppends();
        rejectsConditionsTheRuntimeCannotComplete();
        roundTripsComplexSupportedQuest();
        fuzzesParserWithoutUnexpectedFailures();
        System.out.println("Quest functional self-test passed (" + checks + " checks).");
    }

    private static void validatesBuilderEquivalentMatrix() {
        String[] expressions = {
                "inventory.count(\"minecraft:diamond\") %s %d",
                "stat.mined(\"minecraft:ancient_debris\") %s %d",
                "stat.killed(\"minecraft:zombie\") %s %d"
        };
        String[] operators = {">=", "==", "<="};
        String[] animations = {"wax_seal", "ink_check", "page_fold"};
        int[] amounts = {0, 1, 1_000_000};

        int serial = 0;
        for (String expression : expressions) {
            for (String operator : operators) {
                for (String animation : animations) {
                    for (int amount : amounts) {
                        String source = quest(
                                "builder_" + serial++,
                                expression.formatted(operator, amount),
                                animation
                        );
                        var parsed = QuestScript.parseAndValidate(source);
                        require(parsed.validation().valid(),
                                "Builder-equivalent quest was rejected: "
                                        + parsed.validation().diagnostics());
                        require(new QuestRuntimeSupportValidator().validate(parsed.file()).valid(),
                                "Builder emitted unsupported runtime expression");
                        String formatted = new QuestScriptUserFormatter().format(parsed.file());
                        require(QuestScript.parseAndValidate(formatted).validation().valid(),
                                "Builder-equivalent round trip failed");
                    }
                }
            }
        }

        String manual = quest("manual", "manual.checked == true", "wax_seal");
        var parsedManual = QuestScript.parseAndValidate(manual);
        require(parsedManual.validation().valid(), "Manual builder quest was rejected");
        require(new QuestRuntimeSupportValidator().validate(parsedManual.file()).valid(),
                "Manual builder quest was not runtime-supported");
    }

    private static void rejectsDuplicateIdsAcrossAppends() {
        QuestDefinition first = QuestScript.parseAndValidate(quest(
                "duplicate_id",
                "inventory.count(\"minecraft:diamond\") >= 1",
                "wax_seal"
        )).file().quests().getFirst();
        QuestDefinition second = QuestScript.parseAndValidate(quest(
                "duplicate_id",
                "inventory.count(\"minecraft:gold_ingot\") >= 1",
                "ink_check"
        )).file().quests().getFirst();

        require(new QuestScriptValidator().validate(new QuestFile(List.of(first))).valid(),
                "First append fragment should be valid alone");
        require(new QuestScriptValidator().validate(new QuestFile(List.of(second))).valid(),
                "Second append fragment should be valid alone");
        require(!new QuestScriptValidator().validate(new QuestFile(List.of(first, second))).valid(),
                "Merged store accepted duplicate quest IDs");
    }

    private static void rejectsConditionsTheRuntimeCannotComplete() {
        String[] unsupported = {
                "world.day >= 1",
                "world.time >= 1000",
                "world.is_day == true",
                "player.biome == \"minecraft:plains\"",
                "player.gamemode == \"survival\"",
                "world.weather == \"clear\"",
                "world.difficulty == \"normal\"",
                "advancement.done(\"minecraft:story/mine_diamond\") == true",
                "quest.done(\"earlier_quest\") == true",
                "inventory.count(\"#minecraft:logs\") >= 1"
        };
        QuestRuntimeSupportValidator validator = new QuestRuntimeSupportValidator();
        for (int index = 0; index < unsupported.length; index++) {
            var parsed = QuestScript.parseAndValidate(quest(
                    "unsupported_" + index,
                    unsupported[index],
                    "wax_seal"
            ));
            require(parsed.validation().valid(),
                    "Deferred syntax should remain parseable for compatibility");
            require(!validator.validate(parsed.file()).valid(),
                    "Save-facing runtime validation accepted: " + unsupported[index]);
        }

        String[] supported = {
                "player.health >= 10",
                "player.dimension == \"minecraft:overworld\"",
                "inventory.has(\"minecraft:diamond\") == true",
                "inventory.equipped(\"minecraft:diamond_helmet\") == true",
                "inventory.durability(\"minecraft:diamond_pickaxe\") >= 1",
                "stat.crafted(\"minecraft:crafting_table\") >= 1",
                "stat.used(\"minecraft:ender_pearl\") >= 1",
                "stat.picked_up(\"minecraft:diamond\") >= 1",
                "stat.dropped(\"minecraft:cobblestone\") >= 1",
                "distance.to(0, 64, 0) <= 8",
                "inside.box(-8, 50, -8, 8, 100, 8) == true",
                "inside.radius(0, 64, 0, 8) == true",
                "quest.active(\"another quest\") == true"
        };
        for (int index = 0; index < supported.length; index++) {
            var parsed = QuestScript.parseAndValidate(quest(
                    "supported_" + index,
                    supported[index],
                    "page_fold"
            ));
            require(parsed.validation().valid(), "Supported expression failed semantics");
            require(validator.validate(parsed.file()).valid(),
                    "Runtime validator rejected: " + supported[index]);
        }
    }

    private static void roundTripsComplexSupportedQuest() {
        String source = """
                quest "복합 기능 점검" {
                  id "full_audit"
                  description "수동·인벤토리·위치·통계를 함께 검사한다."
                  complete when {
                    manual.checked == true
                    and inventory.count("minecraft:gold_ingot") >= 64
                    and inside.radius(100, 64, -30, 8) == true
                    and stat.mined("minecraft:ancient_debris") >= 4
                  }
                  animation "page_fold"
                  hud show
                }
                """;
        var parsed = QuestScript.parseAndValidate(source);
        require(parsed.validation().valid(), "Complex supported quest failed validation");
        require(new QuestRuntimeSupportValidator().validate(parsed.file()).valid(),
                "Complex supported quest failed runtime validation");
        String canonical = new QuestScriptFormatter().format(parsed.file());
        QuestFile reparsed = QuestScript.parse(canonical);
        require(new QuestScriptFormatter().format(reparsed).equals(canonical),
                "Canonical round trip changed the persisted source");
    }

    private static void fuzzesParserWithoutUnexpectedFailures() {
        Random random = new Random(0x514C45444745524CL);
        char[] alphabet = (
                "quest complete when id description animation hud show hide and or not "
                        + "{}()[]\"'.:_#/-+<>=!0123456789abcdef가나다라마바사 \n\t"
        ).toCharArray();
        for (int caseIndex = 0; caseIndex < 20_000; caseIndex++) {
            int length = random.nextInt(160);
            StringBuilder source = new StringBuilder(length);
            for (int index = 0; index < length; index++) {
                source.append(alphabet[random.nextInt(alphabet.length)]);
            }
            try {
                QuestScript.parseAndValidate(source.toString());
            } catch (QuestScriptException expected) {
                // Invalid random source is expected; no other Throwable may escape.
            }
            checks++;
        }
    }

    private static String quest(String id, String expression, String animation) {
        return """
                quest "기능 점검" {
                  id "%s"
                  complete when {
                    %s
                  }
                  animation "%s"
                  hud show
                }
                """.formatted(id, expression, animation);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
