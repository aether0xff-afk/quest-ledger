package dev.aether.questledger.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aether.questledger.QuestLedger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class QuestLedgerClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(QuestLedger.MOD_ID, "general")
    );

    private KeyMapping openLedger;

    @Override
    public void onInitializeClient() {
        this.openLedger = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.questledger.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_Q,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            QuestCompletionController.tick(client);
            while (this.openLedger.consumeClick()) {
                if (ClientQuestStore.activeScope().isPresent()) {
                    client.gui.setScreen(new QuestLedgerScreen(null));
                }
            }
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(QuestLedger.MOD_ID, "quest_list"),
                QuestLedgerHud::extract
        );

        QuestLedger.LOGGER.info(
                "Quest Ledger client initialized with scoped storage and "
                        + "backend-neutral Blaze3D GUI rendering."
        );
    }
}
