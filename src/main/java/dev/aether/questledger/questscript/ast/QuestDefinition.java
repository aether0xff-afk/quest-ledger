package dev.aether.questledger.questscript.ast;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record QuestDefinition(
        String title,
        Optional<String> id,
        Optional<String> description,
        Expression completionCondition,
        Duration holdDuration,
        Duration removeAfter,
        Optional<String> animation,
        boolean hudVisible
) {
    public QuestDefinition {
        Objects.requireNonNull(title);
        id = id == null ? Optional.empty() : id;
        description = description == null ? Optional.empty() : description;
        Objects.requireNonNull(completionCondition);
        holdDuration = holdDuration == null ? Duration.ZERO : holdDuration;
        removeAfter = removeAfter == null ? Duration.ofMillis(1200) : removeAfter;
        animation = animation == null ? Optional.empty() : animation;
    }
}
