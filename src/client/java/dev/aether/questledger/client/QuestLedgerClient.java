package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import net.fabricmc.api.ClientModInitializer;

public final class QuestLedgerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QuestLedger.LOGGER.info("Quest Ledger client initialized.");
    }
}
