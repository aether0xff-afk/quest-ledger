package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.Expression;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public final class QuestStatisticAccess {
    private static final Set<String> SUPPORTED_FUNCTIONS = Set.of(
            "stat.mined",
            "stat.used",
            "stat.crafted",
            "stat.killed",
            "stat.picked_up",
            "stat.dropped"
    );

    private QuestStatisticAccess() {
    }

    public static Set<StatReference> collect(Expression expression) {
        Set<StatReference> references = new LinkedHashSet<>();
        collectInto(expression, references);
        return Set.copyOf(references);
    }

    private static void collectInto(Expression expression, Set<StatReference> references) {
        if (expression instanceof Expression.Logical logical) {
            collectInto(logical.left(), references);
            collectInto(logical.right(), references);
            return;
        }
        if (expression instanceof Expression.Not not) {
            collectInto(not.operand(), references);
            return;
        }
        if (expression instanceof Expression.Comparison comparison) {
            collectInto(comparison.left(), references);
            collectInto(comparison.right(), references);
            return;
        }
        if (expression instanceof Expression.Call call) {
            if (SUPPORTED_FUNCTIONS.contains(call.qualifiedName())
                    && call.arguments().size() == 1
                    && call.arguments().getFirst() instanceof Expression.StringLiteral target) {
                references.add(new StatReference(call.qualifiedName(), target.value()));
            }
            for (Expression argument : call.arguments()) {
                collectInto(argument, references);
            }
        }
    }

    public static OptionalLong read(LocalPlayer player, StatReference reference) {
        return switch (reference.functionName()) {
            case "stat.mined" -> readBlockStat(player, reference.targetId());
            case "stat.used" -> readItemStat(player, reference.targetId(), ItemStat.USED);
            case "stat.crafted" -> readItemStat(player, reference.targetId(), ItemStat.CRAFTED);
            case "stat.picked_up" -> readItemStat(
                    player,
                    reference.targetId(),
                    ItemStat.PICKED_UP
            );
            case "stat.dropped" -> readItemStat(
                    player,
                    reference.targetId(),
                    ItemStat.DROPPED
            );
            case "stat.killed" -> readEntityStat(player, reference.targetId());
            default -> OptionalLong.empty();
        };
    }

    private static OptionalLong readBlockStat(LocalPlayer player, String idText) {
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            return OptionalLong.empty();
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        return block.isEmpty()
                ? OptionalLong.empty()
                : OptionalLong.of(player.getStats().getValue(Stats.BLOCK_MINED.get(block.get())));
    }

    private static OptionalLong readItemStat(
            LocalPlayer player,
            String idText,
            ItemStat kind
    ) {
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            return OptionalLong.empty();
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty()) {
            return OptionalLong.empty();
        }
        int value = switch (kind) {
            case USED -> player.getStats().getValue(Stats.ITEM_USED.get(item.get()));
            case CRAFTED -> player.getStats().getValue(Stats.ITEM_CRAFTED.get(item.get()));
            case PICKED_UP -> player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item.get()));
            case DROPPED -> player.getStats().getValue(Stats.ITEM_DROPPED.get(item.get()));
        };
        return OptionalLong.of(value);
    }

    private static OptionalLong readEntityStat(LocalPlayer player, String idText) {
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            return OptionalLong.empty();
        }
        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        return entityType.isEmpty()
                ? OptionalLong.empty()
                : OptionalLong.of(
                        player.getStats().getValue(Stats.ENTITY_KILLED.get(entityType.get()))
                );
    }

    private enum ItemStat {
        USED,
        CRAFTED,
        PICKED_UP,
        DROPPED
    }

    public record StatReference(String functionName, String targetId) {
        private static final String SEPARATOR = "\u0000";

        public String externalForm() {
            return functionName + SEPARATOR + targetId;
        }

        public static Optional<StatReference> parseExternalForm(String value) {
            int separator = value.indexOf(SEPARATOR);
            if (separator <= 0 || separator == value.length() - 1) {
                return Optional.empty();
            }
            String function = value.substring(0, separator);
            if (!SUPPORTED_FUNCTIONS.contains(function)) {
                return Optional.empty();
            }
            return Optional.of(new StatReference(function, value.substring(separator + 1)));
        }
    }
}
