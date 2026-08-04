package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import dev.aether.questledger.questscript.QuestScriptUserFormatter;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Persists manual confirmation state beside each world/server quest store.
 */
public final class ManualQuestStore {
    private static final Path SCOPES_DIRECTORY = FabricLoader.getInstance().getConfigDir()
            .resolve("quest-ledger")
            .resolve("worlds");

    private static String currentScopeKey;
    private static Path statePath;
    private static Set<String> checkedKeys = Set.of();

    private ManualQuestStore() {
    }

    public static synchronized boolean synchronizeScope(Minecraft minecraft) {
        Optional<QuestWorldScope> resolved = QuestWorldScope.resolve(minecraft);
        if (resolved.isEmpty()) {
            if (currentScopeKey != null) {
                unload();
                return true;
            }
            return false;
        }

        QuestWorldScope scope = resolved.get();
        if (scope.key().equals(currentScopeKey)) {
            return false;
        }

        currentScopeKey = scope.key();
        statePath = SCOPES_DIRECTORY.resolve(scope.directoryName())
                .resolve("manual-state.properties");
        load();
        pruneToActiveQuests();
        return true;
    }

    public static synchronized boolean isChecked(QuestDefinition quest) {
        return checkedKeys.contains(questKey(quest));
    }

    public static synchronized boolean markChecked(QuestDefinition quest) {
        if (statePath == null) {
            return false;
        }
        Set<String> updated = new HashSet<>(checkedKeys);
        boolean changed = updated.add(questKey(quest));
        if (changed) {
            checkedKeys = Set.copyOf(updated);
            persist();
        }
        return changed;
    }

    public static synchronized void clear(QuestDefinition quest) {
        if (statePath == null || checkedKeys.isEmpty()) {
            return;
        }
        Set<String> updated = new HashSet<>(checkedKeys);
        if (updated.remove(questKey(quest))) {
            checkedKeys = Set.copyOf(updated);
            persist();
        }
    }

    private static void unload() {
        currentScopeKey = null;
        statePath = null;
        checkedKeys = Set.of();
    }

    private static void load() {
        if (statePath == null || !Files.exists(statePath)) {
            checkedKeys = Set.of();
            return;
        }

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(Files.readString(
                    statePath,
                    StandardCharsets.UTF_8
            )));
            Set<String> loaded = new HashSet<>();
            for (String name : properties.stringPropertyNames()) {
                if (!name.startsWith("checked.")) {
                    continue;
                }
                decode(name.substring("checked.".length())).ifPresent(loaded::add);
            }
            checkedKeys = Set.copyOf(loaded);
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not load manual quest state", exception);
            checkedKeys = Set.of();
        }
    }

    private static void pruneToActiveQuests() {
        if (checkedKeys.isEmpty()) {
            return;
        }
        Set<String> active = new HashSet<>();
        for (QuestDefinition quest : ClientQuestStore.snapshot().quests()) {
            active.add(questKey(quest));
        }
        Set<String> updated = new HashSet<>(checkedKeys);
        if (updated.retainAll(active)) {
            checkedKeys = Set.copyOf(updated);
            persist();
        }
    }

    private static void persist() {
        if (statePath == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        for (String key : checkedKeys) {
            properties.setProperty("checked." + encode(key), "true");
        }

        try {
            StringWriter writer = new StringWriter();
            properties.store(writer, "Quest Ledger manual completion confirmations");
            writeAtomically(statePath, writer.toString());
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not save manual quest state", exception);
        }
    }

    private static String questKey(QuestDefinition quest) {
        if (quest.id().isPresent()) {
            return "id:" + quest.id().get();
        }

        String fingerprint = fingerprint(quest);
        int occurrence = 0;
        for (QuestDefinition candidate : ClientQuestStore.snapshot().quests()) {
            if (candidate == quest) {
                break;
            }
            if (fingerprint(candidate).equals(fingerprint)) {
                occurrence++;
            }
        }
        return "fingerprint:" + fingerprint + ":" + occurrence;
    }

    private static String fingerprint(QuestDefinition quest) {
        String canonical = new QuestScriptUserFormatter().format(
                new QuestFile(java.util.List.of(quest))
        );
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Optional<String> decode(String value) {
        try {
            return Optional.of(new String(
                    Base64.getUrlDecoder().decode(value),
                    StandardCharsets.UTF_8
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void writeAtomically(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        try {
            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
