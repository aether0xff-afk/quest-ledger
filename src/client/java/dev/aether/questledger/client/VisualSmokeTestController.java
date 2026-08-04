package dev.aether.questledger.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.aether.questledger.QuestLedger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Temporary CI-only visual smoke-test controller. It is completely inert unless
 * QUEST_LEDGER_VISUAL_SMOKE_TEST=1 is present in the process environment.
 */
public final class VisualSmokeTestController {
    private static final boolean ENABLED = "1".equals(
            System.getenv("QUEST_LEDGER_VISUAL_SMOKE_TEST")
    );
    private static final int MAX_DEPTH = 7;
    private static final int MAX_VISITED_OBJECTS = 20_000;

    private static int ticks;

    private VisualSmokeTestController() {
    }

    public static void tick(Minecraft client) {
        if (!ENABLED) {
            return;
        }

        ticks++;
        if (ticks == 80) {
            client.gui.setScreen(new QuestLedgerScreen(null));
        } else if (ticks == 150) {
            captureAll(client, "builder", "quest-ledger-builder-ready");
        } else if (ticks == 330) {
            client.gui.setScreen(new QuestTypesScreen(null));
        } else if (ticks == 400) {
            captureAll(client, "types", "quest-ledger-types-ready");
        } else if (ticks == 650) {
            client.stop();
        }
    }

    private static void captureAll(Minecraft client, String prefix, String markerName) {
        List<TargetCandidate> targets = findRenderTargets(client);
        QuestLedger.LOGGER.info(
                "Visual smoke-test found {} RenderTarget candidate(s) for {}",
                targets.size(),
                prefix
        );

        Path diagnostics = client.gameDirectory.toPath().resolve(prefix + "-targets.txt");
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < targets.size(); index++) {
            TargetCandidate candidate = targets.get(index);
            lines.add(index + "\t" + candidate.path() + "\t"
                    + candidate.target().getClass().getName() + "\t"
                    + String.valueOf(candidate.target()));
        }
        try {
            Files.write(diagnostics, lines);
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not write RenderTarget diagnostics", exception);
        }

        if (targets.isEmpty()) {
            marker(markerName);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(targets.size());
        for (int index = 0; index < targets.size(); index++) {
            TargetCandidate candidate = targets.get(index);
            String fileName = prefix + "-target-" + index + ".png";
            QuestLedger.LOGGER.info(
                    "Capturing {} from RenderTarget path {}",
                    fileName,
                    candidate.path()
            );
            Screenshot.grab(
                    client.gameDirectory,
                    fileName,
                    candidate.target(),
                    1,
                    message -> {
                        QuestLedger.LOGGER.info(
                                "Visual smoke-test screenshot {}: {}",
                                fileName,
                                message.getString()
                        );
                        if (remaining.decrementAndGet() == 0) {
                            marker(markerName);
                        }
                    }
            );
        }
    }

    private static List<TargetCandidate> findRenderTargets(Object root) {
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        IdentityHashMap<RenderTarget, String> found = new IdentityHashMap<>();
        ArrayDeque<ObjectNode> queue = new ArrayDeque<>();
        queue.addLast(new ObjectNode(root, "Minecraft", 0));

        while (!queue.isEmpty() && visited.size() < MAX_VISITED_OBJECTS) {
            ObjectNode node = queue.removeFirst();
            Object value = node.value();
            if (value == null || visited.put(value, Boolean.TRUE) != null) {
                continue;
            }
            if (value instanceof RenderTarget target) {
                found.putIfAbsent(target, node.path());
                continue;
            }
            if (node.depth() >= MAX_DEPTH) {
                continue;
            }

            Class<?> type = value.getClass();
            if (type.isArray()) {
                int length = Math.min(Array.getLength(value), 512);
                for (int index = 0; index < length; index++) {
                    enqueue(queue, Array.get(value, index),
                            node.path() + "[" + index + "]", node.depth() + 1);
                }
                continue;
            }
            if (value instanceof Iterable<?> iterable) {
                int index = 0;
                for (Object element : iterable) {
                    if (index >= 512) break;
                    enqueue(queue, element, node.path() + "[" + index + "]",
                            node.depth() + 1);
                    index++;
                }
                continue;
            }
            if (value instanceof Map<?, ?> map) {
                int index = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (index >= 512) break;
                    enqueue(queue, entry.getKey(), node.path() + ".key[" + index + "]",
                            node.depth() + 1);
                    enqueue(queue, entry.getValue(), node.path() + ".value[" + index + "]",
                            node.depth() + 1);
                    index++;
                }
                continue;
            }
            if (!isMinecraftObject(type)) {
                continue;
            }

            for (Class<?> cursor = type; cursor != null && cursor != Object.class;
                    cursor = cursor.getSuperclass()) {
                for (Field field : cursor.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())
                            || field.getType().isPrimitive()
                            || field.getType().isEnum()) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object nested = field.get(value);
                        enqueue(queue, nested, node.path() + "." + field.getName(),
                                node.depth() + 1);
                    } catch (Throwable ignored) {
                        // Some JDK and native-backed fields are intentionally inaccessible.
                    }
                }
            }
        }

        List<TargetCandidate> result = new ArrayList<>();
        found.forEach((target, path) -> result.add(new TargetCandidate(target, path)));
        return result;
    }

    private static void enqueue(
            ArrayDeque<ObjectNode> queue,
            Object value,
            String path,
            int depth
    ) {
        if (value != null) {
            queue.addLast(new ObjectNode(value, path, depth));
        }
    }

    private static boolean isMinecraftObject(Class<?> type) {
        String name = type.getName();
        return name.startsWith("net.minecraft.")
                || name.startsWith("com.mojang.blaze3d.")
                || name.startsWith("java.util.");
    }

    private static void marker(String fileName) {
        try {
            Files.writeString(Path.of(fileName), "ready\n");
            QuestLedger.LOGGER.info("Visual smoke-test marker ready: {}", fileName);
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not write visual smoke-test marker", exception);
        }
    }

    private record ObjectNode(Object value, String path, int depth) { }
    private record TargetCandidate(RenderTarget target, String path) { }
}
