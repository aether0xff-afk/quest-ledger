package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CI-only real-world functional smoke controller. It is inert during normal play and only
 * activates when the functional workflow supplies QUEST_LEDGER_FUNCTIONAL_SMOKE_PHASE.
 */
public final class FunctionalSmokeTestController {
    private static final String PHASE = System.getenv("QUEST_LEDGER_FUNCTIONAL_SMOKE_PHASE");
    private static final boolean ENABLED = "1".equals(PHASE) || "2".equals(PHASE);

    private static int worldTicks;
    private static boolean initialized;
    private static boolean completionAnimationObserved;
    private static boolean finished;

    private FunctionalSmokeTestController() {
    }

    public static void tick(Minecraft client) {
        if (!ENABLED || finished) {
            return;
        }
        if (client.player == null || client.level == null
                || ClientQuestStore.activeScope().isEmpty()) {
            return;
        }

        worldTicks++;
        if (QuestCompletionController.completionView().isPresent()) {
            completionAnimationObserved = true;
        }

        try {
            if (!initialized) {
                initialized = true;
                if ("1".equals(PHASE)) {
                    beginFirstPass();
                } else {
                    beginSecondPass(client);
                }
            }

            if ("1".equals(PHASE) && worldTicks >= 160) {
                verifyFirstPass(client);
            } else if ("2".equals(PHASE) && worldTicks >= 30) {
                finishSecondPass(client);
            }
        } catch (Throwable failure) {
            fail(client, failure);
        }
    }

    private static void beginFirstPass() {
        require(ClientQuestStore.replace("").success(), "Could not clear the test scope");

        ClientQuestStore.SaveResult first = ClientQuestStore.append(quest(
                "persistent_target",
                "영구 대상",
                "inventory.count(\"minecraft:bedrock\") >= 1",
                "wax_seal"
        ));
        require(first.success(), "Could not save persistent target: " + first.message());

        ClientQuestStore.SaveResult duplicate = ClientQuestStore.append(quest(
                "persistent_target",
                "중복 대상",
                "inventory.count(\"minecraft:diamond\") >= 1",
                "wax_seal"
        ));
        require(!duplicate.success() && duplicate.message().contains("DUPLICATE_QUEST_ID"),
                "Duplicate quest ID was not rejected: " + duplicate.message());
        require(ClientQuestStore.snapshot().quests().size() == 1,
                "Rejected duplicate changed the active store");

        ClientQuestStore.SaveResult unsupported = ClientQuestStore.append(quest(
                "unsupported_clock",
                "미지원 조건",
                "world.day >= 1",
                "wax_seal"
        ));
        require(!unsupported.success() && unsupported.message().contains("UNSUPPORTED_RUNTIME"),
                "Unsupported runtime condition was not rejected: " + unsupported.message());
        require(ClientQuestStore.snapshot().quests().size() == 1,
                "Rejected runtime condition changed the active store");

        require(ClientQuestStore.append(quest(
                "active_watcher",
                "활성 퀘스트 감지",
                "quest.active(\"persistent_target\") == true",
                "ink_check"
        )).success(), "quest.active could not be saved");

        require(ClientQuestStore.append(quest(
                "manual_target",
                "수동 확인",
                "manual.checked == true",
                "page_fold"
        )).success(), "Manual quest could not be saved");

        require(ClientQuestStore.append(quest(
                "inventory_target",
                "인벤토리 자동 판정",
                "inventory.count(\"minecraft:stone\") >= 0",
                "wax_seal"
        )).success(), "Inventory quest could not be saved");

        require(ClientQuestStore.append(quest(
                "stat_target",
                "통계 자동 판정",
                "stat.mined(\"minecraft:stone\") >= 0",
                "ink_check"
        )).success(), "Statistic quest could not be saved");

        QuestDefinition manual = findById("manual_target");
        require(ManualQuestStore.markChecked(manual), "Manual confirmation was not persisted");
    }

    private static void verifyFirstPass(Minecraft client) throws IOException {
        List<QuestDefinition> active = ClientQuestStore.snapshot().quests();
        require(active.size() == 1, "Expected one persistent quest, found " + active.size());
        require(active.getFirst().id().orElse("").equals("persistent_target"),
                "Unexpected quest survived automatic completion");
        require(completionAnimationObserved, "No completion animation state was observed");

        Path scopeDirectory = scopeDirectory();
        require(Files.exists(scopeDirectory.resolve("quests.qs")), "quests.qs was not written");
        require(Files.exists(scopeDirectory.resolve("runtime-state.properties")),
                "runtime-state.properties was not written");
        require(Files.exists(scopeDirectory.resolve("manual-state.properties")),
                "manual-state.properties was not written");
        Path history = scopeDirectory.resolve("completed-history.log");
        require(Files.exists(history), "Completion history was not written");
        long historyLines;
        try (var lines = Files.lines(history, StandardCharsets.UTF_8)) {
            historyLines = lines.count();
        }
        require(historyLines >= 4, "Expected at least four completed quests in history");
        pass(client, "functional-smoke-phase-1-success");
    }

    private static void beginSecondPass(Minecraft client) {
        List<QuestDefinition> restored = ClientQuestStore.snapshot().quests();
        require(restored.size() == 1, "Restart did not restore exactly one quest");
        require(restored.getFirst().id().orElse("").equals("persistent_target"),
                "Restart restored the wrong quest");
        require(ClientQuestStore.source().contains("persistent_target"),
                "Restarted canonical source lost the quest ID");
        require(ClientQuestStore.replace("").success(), "Could not clear restored quest");
        require(ClientQuestStore.snapshot().quests().isEmpty(),
                "Cleared quest remained in memory");
        marker(client, "functional-smoke-phase-2-cleared", "cleared\n");
    }

    private static void finishSecondPass(Minecraft client) {
        require(ClientQuestStore.snapshot().quests().isEmpty(),
                "Empty store repopulated after the second-pass clear");
        pass(client, "functional-smoke-phase-2-success");
    }

    private static QuestDefinition findById(String id) {
        return ClientQuestStore.snapshot().quests().stream()
                .filter(quest -> quest.id().map(id::equals).orElse(false))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Quest not found: " + id));
    }

    private static Path scopeDirectory() {
        QuestWorldScope scope = ClientQuestStore.activeScope().orElseThrow();
        return FabricLoader.getInstance().getConfigDir()
                .resolve("quest-ledger")
                .resolve("worlds")
                .resolve(scope.directoryName());
    }

    private static String quest(String id, String title, String condition, String animation) {
        return """
                quest "%s" {
                  id "%s"
                  complete when {
                    %s
                  }
                  animation "%s"
                  hud show
                }
                """.formatted(title, id, condition, animation);
    }

    private static void pass(Minecraft client, String marker) {
        finished = true;
        marker(client, marker, "success\n");
        QuestLedger.LOGGER.info("Quest Ledger functional smoke test passed: {}", marker);
        client.stop();
    }

    private static void fail(Minecraft client, Throwable failure) {
        finished = true;
        String message = failure.getClass().getName() + ": " + failure.getMessage();
        QuestLedger.LOGGER.error("Quest Ledger functional smoke test failed", failure);
        marker(client, "functional-smoke-failure.txt", message + "\n");
        client.stop();
    }

    private static void marker(Minecraft client, String fileName, String content) {
        try {
            Files.writeString(client.gameDirectory.toPath().resolve(fileName), content,
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not write functional smoke marker {}", fileName,
                    exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
