package dev.aether.questledger.client;

import dev.aether.questledger.questscript.ast.ComparisonOperator;
import dev.aether.questledger.questscript.ast.Expression;
import dev.aether.questledger.questscript.ast.LogicalOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public final class QuestConditionEvaluator {
    public Result evaluate(Expression expression, Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return Result.unknown("No active player or level.");
        }

        Value value = valueOf(expression, minecraft, player);
        if (value instanceof Value.BooleanValue booleanValue) {
            return Result.known(booleanValue.value());
        }
        if (value instanceof Value.UnknownValue unknown) {
            return Result.unknown(unknown.reason());
        }
        return Result.unknown("Completion expression did not evaluate to a boolean.");
    }

    private Value valueOf(Expression expression, Minecraft minecraft, LocalPlayer player) {
        if (expression instanceof Expression.BooleanLiteral literal) {
            return new Value.BooleanValue(literal.value());
        }
        if (expression instanceof Expression.NumberLiteral literal) {
            return new Value.NumberValue(literal.value());
        }
        if (expression instanceof Expression.StringLiteral literal) {
            return new Value.StringValue(literal.value());
        }
        if (expression instanceof Expression.Not not) {
            Value operand = valueOf(not.operand(), minecraft, player);
            if (operand instanceof Value.BooleanValue booleanValue) {
                return new Value.BooleanValue(!booleanValue.value());
            }
            return unknownFrom(operand, "not requires a boolean value.");
        }
        if (expression instanceof Expression.Logical logical) {
            Value left = valueOf(logical.left(), minecraft, player);
            if (!(left instanceof Value.BooleanValue leftBoolean)) {
                return unknownFrom(left, "Logical operator requires booleans.");
            }

            if (logical.operator() == LogicalOperator.AND && !leftBoolean.value()) {
                return new Value.BooleanValue(false);
            }
            if (logical.operator() == LogicalOperator.OR && leftBoolean.value()) {
                return new Value.BooleanValue(true);
            }

            Value right = valueOf(logical.right(), minecraft, player);
            if (!(right instanceof Value.BooleanValue rightBoolean)) {
                return unknownFrom(right, "Logical operator requires booleans.");
            }
            return new Value.BooleanValue(logical.operator() == LogicalOperator.AND
                    ? leftBoolean.value() && rightBoolean.value()
                    : leftBoolean.value() || rightBoolean.value());
        }
        if (expression instanceof Expression.Comparison comparison) {
            Value left = valueOf(comparison.left(), minecraft, player);
            Value right = valueOf(comparison.right(), minecraft, player);
            return compare(left, comparison.operator(), right);
        }
        if (expression instanceof Expression.Reference reference) {
            return referenceValue(reference.qualifiedName(), minecraft, player);
        }
        if (expression instanceof Expression.Call call) {
            return callValue(call, minecraft, player);
        }
        return new Value.UnknownValue("Unsupported expression node.");
    }

    private Value referenceValue(String name, Minecraft minecraft, LocalPlayer player) {
        return switch (name) {
            case "player.health" -> new Value.NumberValue(player.getHealth());
            case "player.max_health" -> new Value.NumberValue(player.getMaxHealth());
            case "player.hunger" -> new Value.NumberValue(player.getFoodData().getFoodLevel());
            case "player.experience_level" -> new Value.NumberValue(player.experienceLevel);
            case "player.x" -> new Value.NumberValue(player.getX());
            case "player.y" -> new Value.NumberValue(player.getY());
            case "player.z" -> new Value.NumberValue(player.getZ());
            case "player.dimension" -> new Value.StringValue(
                    player.level().dimension().identifier().toString()
            );
            case "player.on_ground" -> new Value.BooleanValue(player.onGround());
            case "player.is_sneaking" -> new Value.BooleanValue(player.isShiftKeyDown());
            case "player.is_sprinting" -> new Value.BooleanValue(player.isSprinting());
            case "world.time" -> new Value.NumberValue(dayTime(minecraft));
            case "world.day" -> new Value.NumberValue(dayTime(minecraft) / 24_000L);
            case "world.is_day" -> new Value.BooleanValue(isDay(minecraft));
            case "world.is_night" -> new Value.BooleanValue(!isDay(minecraft));
            case "manual.checked" -> new Value.BooleanValue(false);
            default -> new Value.UnknownValue("Runtime property is not implemented: " + name);
        };
    }

    private Value callValue(Expression.Call call, Minecraft minecraft, LocalPlayer player) {
        List<Value> arguments = call.arguments().stream()
                .map(argument -> valueOf(argument, minecraft, player))
                .toList();
        for (Value argument : arguments) {
            if (argument instanceof Value.UnknownValue unknown) {
                return unknown;
            }
        }

        return switch (call.qualifiedName()) {
            case "inventory.count" -> inventoryCount(player, stringArgument(arguments, 0));
            case "inventory.has" -> booleanFromNumber(
                    inventoryCount(player, stringArgument(arguments, 0)),
                    value -> value > 0
            );
            case "inventory.equipped" -> equipped(player, stringArgument(arguments, 0));
            case "inventory.durability" -> durability(player, stringArgument(arguments, 0));
            case "stat.mined" -> blockStat(player, stringArgument(arguments, 0));
            case "stat.used" -> itemStat(player, stringArgument(arguments, 0), StatKind.USED);
            case "stat.crafted" -> itemStat(player, stringArgument(arguments, 0), StatKind.CRAFTED);
            case "stat.picked_up" -> itemStat(player, stringArgument(arguments, 0), StatKind.PICKED_UP);
            case "stat.dropped" -> itemStat(player, stringArgument(arguments, 0), StatKind.DROPPED);
            case "stat.killed" -> entityStat(player, stringArgument(arguments, 0));
            case "distance.to" -> distanceTo(player, arguments);
            case "inside.box" -> insideBox(player, arguments);
            case "inside.radius" -> insideRadius(player, arguments);
            case "quest.active" -> questActive(stringArgument(arguments, 0));
            case "quest.done", "advancement.done" -> new Value.UnknownValue(
                    "Runtime function is not implemented yet: " + call.qualifiedName()
            );
            default -> new Value.UnknownValue(
                    "Runtime function is not implemented: " + call.qualifiedName()
            );
        };
    }

    private Value inventoryCount(LocalPlayer player, String idText) {
        Optional<Item> item = item(idText);
        if (item.isEmpty()) {
            return new Value.UnknownValue("Unknown or unsupported item ID: " + idText);
        }
        return new Value.NumberValue(player.getInventory().countItem(item.get()));
    }

    private Value equipped(LocalPlayer player, String idText) {
        Optional<Item> item = item(idText);
        if (item.isEmpty()) {
            return new Value.UnknownValue("Unknown or unsupported item ID: " + idText);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(item.get())) {
                return new Value.BooleanValue(true);
            }
        }
        return new Value.BooleanValue(false);
    }

    private Value durability(LocalPlayer player, String idText) {
        Optional<Item> item = item(idText);
        if (item.isEmpty()) {
            return new Value.UnknownValue("Unknown or unsupported item ID: " + idText);
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item.get()) && stack.isDamageableItem()) {
                return new Value.NumberValue(stack.getMaxDamage() - stack.getDamageValue());
            }
        }
        return new Value.NumberValue(0);
    }

    private Value blockStat(LocalPlayer player, String idText) {
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            return new Value.UnknownValue("Invalid block ID: " + idText);
        }
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isEmpty()) {
            return new Value.UnknownValue("Unknown block ID: " + idText);
        }
        return new Value.NumberValue(player.getStats().getValue(Stats.BLOCK_MINED.get(block.get())));
    }

    private Value itemStat(LocalPlayer player, String idText, StatKind kind) {
        Optional<Item> item = item(idText);
        if (item.isEmpty()) {
            return new Value.UnknownValue("Unknown item ID: " + idText);
        }
        int value = switch (kind) {
            case USED -> player.getStats().getValue(Stats.ITEM_USED.get(item.get()));
            case CRAFTED -> player.getStats().getValue(Stats.ITEM_CRAFTED.get(item.get()));
            case PICKED_UP -> player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item.get()));
            case DROPPED -> player.getStats().getValue(Stats.ITEM_DROPPED.get(item.get()));
        };
        return new Value.NumberValue(value);
    }

    private Value entityStat(LocalPlayer player, String idText) {
        Identifier id = Identifier.tryParse(idText);
        if (id == null) {
            return new Value.UnknownValue("Invalid entity ID: " + idText);
        }
        Optional<EntityType<?>> entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (entity.isEmpty()) {
            return new Value.UnknownValue("Unknown entity ID: " + idText);
        }
        return new Value.NumberValue(player.getStats().getValue(Stats.ENTITY_KILLED.get(entity.get())));
    }

    private Value distanceTo(LocalPlayer player, List<Value> arguments) {
        double dx = player.getX() - numberArgument(arguments, 0);
        double dy = player.getY() - numberArgument(arguments, 1);
        double dz = player.getZ() - numberArgument(arguments, 2);
        return new Value.NumberValue(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private Value insideBox(LocalPlayer player, List<Value> arguments) {
        double x1 = numberArgument(arguments, 0);
        double y1 = numberArgument(arguments, 1);
        double z1 = numberArgument(arguments, 2);
        double x2 = numberArgument(arguments, 3);
        double y2 = numberArgument(arguments, 4);
        double z2 = numberArgument(arguments, 5);
        return new Value.BooleanValue(
                between(player.getX(), x1, x2)
                        && between(player.getY(), y1, y2)
                        && between(player.getZ(), z1, z2)
        );
    }

    private Value insideRadius(LocalPlayer player, List<Value> arguments) {
        double radius = numberArgument(arguments, 3);
        Value distance = distanceTo(player, arguments.subList(0, 3));
        return booleanFromNumber(distance, value -> value <= radius);
    }

    private Value questActive(String idOrTitle) {
        boolean active = ClientQuestStore.snapshot().quests().stream().anyMatch(quest ->
                quest.id().map(idOrTitle::equals).orElse(false) || quest.title().equals(idOrTitle)
        );
        return new Value.BooleanValue(active);
    }

    private Optional<Item> item(String idText) {
        if (idText.startsWith("#")) {
            return Optional.empty();
        }
        Identifier id = Identifier.tryParse(idText);
        return id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
    }

    private long dayTime(Minecraft minecraft) {
        return minecraft.level.getLevelData().getTimeOfDay();
    }

    private boolean isDay(Minecraft minecraft) {
        long timeOfDay = Math.floorMod(dayTime(minecraft), 24_000L);
        return timeOfDay < 13_000L;
    }

    private Value compare(Value left, ComparisonOperator operator, Value right) {
        if (left instanceof Value.UnknownValue unknown) {
            return unknown;
        }
        if (right instanceof Value.UnknownValue unknown) {
            return unknown;
        }
        if (left instanceof Value.NumberValue leftNumber
                && right instanceof Value.NumberValue rightNumber) {
            double comparison = Double.compare(leftNumber.value(), rightNumber.value());
            return new Value.BooleanValue(compareNumber(comparison, operator));
        }
        if (left instanceof Value.StringValue leftString
                && right instanceof Value.StringValue rightString) {
            return equalityComparison(leftString.value().equals(rightString.value()), operator);
        }
        if (left instanceof Value.BooleanValue leftBoolean
                && right instanceof Value.BooleanValue rightBoolean) {
            return equalityComparison(leftBoolean.value() == rightBoolean.value(), operator);
        }
        return new Value.UnknownValue("Cannot compare runtime values of different types.");
    }

    private boolean compareNumber(double comparison, ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> comparison == 0;
            case NOT_EQUAL -> comparison != 0;
            case GREATER -> comparison > 0;
            case GREATER_EQUAL -> comparison >= 0;
            case LESS -> comparison < 0;
            case LESS_EQUAL -> comparison <= 0;
        };
    }

    private Value equalityComparison(boolean equal, ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> new Value.BooleanValue(equal);
            case NOT_EQUAL -> new Value.BooleanValue(!equal);
            default -> new Value.UnknownValue("Only == and != are valid for this runtime value.");
        };
    }

    private Value booleanFromNumber(Value value, NumberPredicate predicate) {
        if (value instanceof Value.NumberValue number) {
            return new Value.BooleanValue(predicate.test(number.value()));
        }
        return value;
    }

    private Value unknownFrom(Value value, String fallback) {
        return value instanceof Value.UnknownValue unknown
                ? unknown
                : new Value.UnknownValue(fallback);
    }

    private String stringArgument(List<Value> values, int index) {
        return ((Value.StringValue) values.get(index)).value();
    }

    private double numberArgument(List<Value> values, int index) {
        return ((Value.NumberValue) values.get(index)).value();
    }

    private boolean between(double value, double endpointA, double endpointB) {
        return value >= Math.min(endpointA, endpointB) && value <= Math.max(endpointA, endpointB);
    }

    private enum StatKind {
        USED,
        CRAFTED,
        PICKED_UP,
        DROPPED
    }

    @FunctionalInterface
    private interface NumberPredicate {
        boolean test(double value);
    }

    private sealed interface Value {
        record NumberValue(double value) implements Value { }
        record StringValue(String value) implements Value { }
        record BooleanValue(boolean value) implements Value { }
        record UnknownValue(String reason) implements Value { }
    }

    public record Result(boolean known, boolean satisfied, String reason) {
        public static Result known(boolean satisfied) {
            return new Result(true, satisfied, "");
        }

        public static Result unknown(String reason) {
            return new Result(false, false, reason);
        }
    }
}
