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
    private static final long MINIMUM_ANIMATION_MILLIS = 450L;

    private static final QuestConditionEvaluator EVALUATOR = new QuestConditionEvaluator();
    private static final Map<QuestDefinition, State> STATES = new HashMap<>();
    private static final Map<String, Boolean> REPORTED_UNKNOWN = new HashMap<>();

    private static int ticks;

    private QuestCompletionController() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
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

            QuestConditionEvaluator.Result result = EVALUATOR.evaluate(
                    quest.completionCondition(),
                    minecraft
            );
            if (!result.known()) {
                state.satisfiedSince = 0L;
                reportUnknownOnce(quest, result.reason());
                continue;
            }

            if (!result.satisfied()) {
                state.satisfiedSince = 0L;
                continue;
            }

            if (state.satisfiedSince == 0L) {
                state.satisfiedSince = now;
            }

            long requiredHold = Math.max(0L, quest.holdDuration().toMillis());
            if (now - state.satisfiedSince >= requiredHold) {
                state.completionStartedAt = now;
                state.animationDuration = Math.max(
                        MINIMUM_ANIMATION_MILLIS,
                        quest.removeAfter().toMillis()
                );
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
            if (now - state.completionStartedAt < state.animationDuration) {
                continue;
            }

            ClientQuestStore.SaveResult result = ClientQuestStore.complete(entry.getKey());
            if (!result.success()) {
                QuestLedger.LOGGER.error(
                        "Could not remove completed quest '{}': {}",
                        entry.getKey().title(),
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
                                            / (float) state.animationDuration
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
        private long satisfiedSince;
        private long completionStartedAt;
        private long animationDuration;
    }

    public record CompletionView(QuestDefinition quest, float progress) {
    }
}
