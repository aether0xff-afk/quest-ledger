package dev.aether.questledger.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/** Requests the server's current vanilla statistics so creation-relative quests use live data. */
public final class QuestStatisticSynchronizer {
    private static final int REQUEST_INTERVAL_TICKS = 100;

    private static String scopeKey = "";
    private static int ticksUntilRequest;

    private QuestStatisticSynchronizer() {
    }

    public static void tick(Minecraft client) {
        if (client.getConnection() == null || ClientQuestStore.activeScope().isEmpty()) {
            reset();
            return;
        }

        String currentScope = ClientQuestStore.activeScope().orElseThrow().key();
        if (!scopeKey.equals(currentScope)) {
            scopeKey = currentScope;
            ticksUntilRequest = 0;
        }

        if (ticksUntilRequest > 0) {
            ticksUntilRequest--;
            return;
        }

        client.getConnection().send(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.REQUEST_STATS
        ));
        ticksUntilRequest = REQUEST_INTERVAL_TICKS;
    }

    private static void reset() {
        scopeKey = "";
        ticksUntilRequest = 0;
    }
}
