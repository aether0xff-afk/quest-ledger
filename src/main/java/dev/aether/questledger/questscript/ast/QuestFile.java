package dev.aether.questledger.questscript.ast;

import java.util.List;

public record QuestFile(List<QuestDefinition> quests) {
    public QuestFile {
        quests = List.copyOf(quests);
    }
}
