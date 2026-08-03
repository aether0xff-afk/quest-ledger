package dev.aether.questledger.questscript.validation;

import dev.aether.questledger.questscript.ast.ValueType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestScriptRegistry {
    private QuestScriptRegistry() { }

    public static final Map<String, ValueType> PROPERTIES = Map.ofEntries(
            Map.entry("player.health", ValueType.NUMBER),
            Map.entry("player.max_health", ValueType.NUMBER),
            Map.entry("player.hunger", ValueType.NUMBER),
            Map.entry("player.experience_level", ValueType.NUMBER),
            Map.entry("player.x", ValueType.NUMBER),
            Map.entry("player.y", ValueType.NUMBER),
            Map.entry("player.z", ValueType.NUMBER),
            Map.entry("player.dimension", ValueType.STRING),
            Map.entry("player.biome", ValueType.STRING),
            Map.entry("player.gamemode", ValueType.STRING),
            Map.entry("player.on_ground", ValueType.BOOLEAN),
            Map.entry("player.is_sneaking", ValueType.BOOLEAN),
            Map.entry("player.is_sprinting", ValueType.BOOLEAN),
            Map.entry("world.day", ValueType.NUMBER),
            Map.entry("world.time", ValueType.NUMBER),
            Map.entry("world.weather", ValueType.STRING),
            Map.entry("world.difficulty", ValueType.STRING),
            Map.entry("world.is_day", ValueType.BOOLEAN),
            Map.entry("world.is_night", ValueType.BOOLEAN),
            Map.entry("manual.checked", ValueType.BOOLEAN)
    );

    public static final Map<String, FunctionSignature> FUNCTIONS = Map.ofEntries(
            stringTo("inventory.count", ValueType.NUMBER, true),
            stringTo("inventory.has", ValueType.BOOLEAN, true),
            stringTo("inventory.equipped", ValueType.BOOLEAN, true),
            stringTo("inventory.durability", ValueType.NUMBER, true),
            stringTo("stat.mined", ValueType.NUMBER, true),
            stringTo("stat.used", ValueType.NUMBER, true),
            stringTo("stat.crafted", ValueType.NUMBER, true),
            stringTo("stat.killed", ValueType.NUMBER, true),
            stringTo("stat.picked_up", ValueType.NUMBER, true),
            stringTo("stat.dropped", ValueType.NUMBER, true),
            stringTo("advancement.done", ValueType.BOOLEAN, true),
            stringTo("quest.done", ValueType.BOOLEAN, false),
            stringTo("quest.active", ValueType.BOOLEAN, false),
            numbersTo("distance.to", 3, ValueType.NUMBER),
            numbersTo("inside.box", 6, ValueType.BOOLEAN),
            numbersTo("inside.radius", 4, ValueType.BOOLEAN)
    );

    public static final Set<String> ANIMATIONS = Set.of(
            "wax_seal", "ink_check", "page_fold"
    );

    private static Map.Entry<String, FunctionSignature> stringTo(
            String name, ValueType returnType, boolean resourceId) {
        return Map.entry(name, new FunctionSignature(List.of(ValueType.STRING), returnType, resourceId));
    }

    private static Map.Entry<String, FunctionSignature> numbersTo(
            String name, int count, ValueType returnType) {
        return Map.entry(name, new FunctionSignature(
                java.util.Collections.nCopies(count, ValueType.NUMBER), returnType, false));
    }
}
