package dev.aether.questledger;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** CI-only server-side statistic driver for the real client integration workflow. */
public final class FunctionalSmokeTestServerController {
    private static final boolean ENABLED = "1".equals(
            System.getenv("QUEST_LEDGER_FUNCTIONAL_SERVER")
    );
    private static final Map<UUID, Integer> JOIN_TICKS = new HashMap<>();
    private static int ticks;

    private FunctionalSmokeTestServerController() {
    }

    public static void register() {
        if (ENABLED) {
            ServerTickEvents.END_SERVER_TICK.register(
                    FunctionalSmokeTestServerController::tick
            );
        }
    }

    private static void tick(MinecraftServer server) {
        ticks++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int joinedAt = JOIN_TICKS.computeIfAbsent(player.getUUID(), ignored -> ticks);
            int age = ticks - joinedAt;
            if (age == 20) {
                player.getStats().setValue(
                        player,
                        Stats.BLOCK_MINED.get(Blocks.STONE),
                        40
                );
                QuestLedger.LOGGER.info("Functional smoke stat baseline set to 40");
            } else if (age == 180) {
                player.getStats().setValue(
                        player,
                        Stats.BLOCK_MINED.get(Blocks.STONE),
                        43
                );
                QuestLedger.LOGGER.info("Functional smoke stat advanced to 43");
            }
        }
    }
}
