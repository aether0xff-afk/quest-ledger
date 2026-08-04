package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class QuestCompletionController {
    private static final int EVALUATION_INTERVAL_TICKS = 5;
    private static final long COMPLETION_ANIMATION_MILLIS = 700L;

    private static final QuestConditionEvaluator EVALUATOR = new QuestConditionEvaluator();
    private static final Map<QuestDefinition, State> STATES = new HashMap<>();
    private static final Map<String, Boolean> REPORTED_UNKNOWN = new HashMap<>();

    private static int ticks;

    private QuestCompletionController() {
    }

    public static void tick(Minecraft minecraft) {
        boolean questScopeChanged = ClientQuestStore.synchronizeScope(minecraft);
        boolean manualScopeChanged = ManualQuestStore.synchronizeScope(minecraft);
        if (questScopeChanged || manualScopeChanged) {
            STATES.clear();
            REPORTED_UNKNOWN.clear();
            ticks = 0;
        }

        if (minecraft.player == null
                || minecraft.level == null
                || ClientQuestStore.activeScope().isEmpty()) {
            STATES.clear();
            return;
        }

        ticks++;
        long now = Util.getMillis();
        finishElapsedAnimations(now);

        if (ticks % EVALUATION_INTERVAL_TICKS != 0) {
            return;
        }

        List<QuestDefinition> active = ClientQuestStore.snapshot().quests();
        STATES.keySet().removeIf(quest -> !active.contains(quest));

        for (QuestDefinition quest : active) {
            State state = STATES.computeIfAbsent(quest, ignored -> new State());
            if (state.completionStartedAt > 0L) {
                continue;
            }

            QuestConditionEvaluator.Result result = EVALUATOR.evaluate(quest, minecraft);
            if (!result.known()) {
                reportUnknownOnce(quest, result.reason());
                continue;
            }

            if (result.satisfied()) {
                state.completionStartedAt = now;
            }
        }
    }

    private static void finishElapsedAnimations(long now) {
        Iterator<Map.Entry<QuestDefinition, State>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<QuestDefinition, State> entry = iterator.next();
            State state = entry.getValue();
            if (state.completionStartedAt <= 0L) {
                continue;
            }
            if (now - state.completionStartedAt < COMPLETION_ANIMATION_MILLIS) {
                continue;
            }

            QuestDefinition quest = entry.getKey();
            boolean manuallyChecked = ManualQuestStore.isChecked(quest);
            if (manuallyChecked) {
                ManualQuestStore.clear(quest);
            }

            ClientQuestStore.SaveResult result = ClientQuestStore.complete(quest);
            if (!result.success()) {
                if (manuallyChecked) {
                    ManualQuestStore.markChecked(quest);
                }
                QuestLedger.LOGGER.error(
                        "Could not remove completed quest '{}': {}",
                        quest.title(),
                        result.message()
                );
            }
            iterator.remove();
        }
    }

    private static void reportUnknownOnce(QuestDefinition quest, String reason) {
        String key = quest.id().orElse(quest.title()) + "\u0000" + reason;
        if (REPORTED_UNKNOWN.putIfAbsent(key, Boolean.TRUE) == null) {
            QuestLedger.LOGGER.warn(
                    "Quest '{}' cannot currently be evaluated: {}",
                    quest.title(),
                    reason
            );
        }
    }

    public static Optional<CompletionView> completionView() {
        long now = Util.getMillis();
        return STATES.entrySet().stream()
                .filter(entry -> entry.getValue().completionStartedAt > 0L)
                .min(Map.Entry.comparingByValue((left, right) ->
                        Long.compare(left.completionStartedAt, right.completionStartedAt)))
                .map(entry -> {
                    State state = entry.getValue();
                    float progress = Math.min(
                            1.0F,
                            Math.max(
                                    0.0F,
                                    (now - state.completionStartedAt)
                                            / (float) COMPLETION_ANIMATION_MILLIS
                            )
                    );
                    return new CompletionView(entry.getKey(), progress);
                });
    }

    public static boolean isCompleting(QuestDefinition quest) {
        State state = STATES.get(quest);
        return state != null && state.completionStartedAt > 0L;
    }

    private static final class State {
        private long completionStartedAt;
    }

    public record CompletionView(QuestDefinition quest, float progress) {
    }
}
