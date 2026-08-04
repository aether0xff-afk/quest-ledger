package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import dev.aether.questledger.client.QuestStatisticAccess.StatReference;
import dev.aether.questledger.questscript.QuestScript;
import dev.aether.questledger.questscript.QuestScriptException;
import dev.aether.questledger.questscript.QuestScriptFormatter;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;
import dev.aether.questledger.questscript.validation.Diagnostic;
import dev.aether.questledger.storage.TransactionalFilePair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class ClientQuestStore {
    private static final Path BASE_DIRECTORY = FabricLoader.getInstance().getConfigDir()
            .resolve("quest-ledger");
    private static final Path SCOPES_DIRECTORY = BASE_DIRECTORY.resolve("worlds");
    private static final Path LEGACY_STORE_PATH = BASE_DIRECTORY.resolve("quests.qs");
    private static final Path LEGACY_HISTORY_PATH = BASE_DIRECTORY.resolve("completed-history.log");
    private static final Path LEGACY_MIGRATION_MARKER = BASE_DIRECTORY
            .resolve("legacy-migration.properties");

    private static QuestWorldScope currentScope;
    private static Path storeDirectory;
    private static Path storePath;
    private static Path runtimePath;
    private static Path historyPath;

    private static QuestFile cached = new QuestFile(List.of());
    private static List<RuntimeEntry> runtimeEntries = List.of();
    private static String source = "";
    private static long changedAtMillis = Util.getMillis();

    private ClientQuestStore() {
    }

    public static synchronized boolean synchronizeScope(Minecraft minecraft) {
        Optional<QuestWorldScope> resolved = QuestWorldScope.resolve(minecraft);
        if (resolved.isEmpty()) {
            if (currentScope != null) {
                unload();
                return true;
            }
            return false;
        }

        QuestWorldScope nextScope = resolved.get();
        if (currentScope != null && currentScope.key().equals(nextScope.key())) {
            return false;
        }

        loadScope(nextScope, minecraft.player);
        return true;
    }

    private static void loadScope(QuestWorldScope scope, LocalPlayer player) {
        currentScope = scope;
        storeDirectory = SCOPES_DIRECTORY.resolve(scope.directoryName());
        storePath = storeDirectory.resolve("quests.qs");
        runtimePath = storeDirectory.resolve("runtime-state.properties");
        historyPath = storeDirectory.resolve("completed-history.log");

        try {
            Files.createDirectories(storeDirectory);
            migrateLegacyStore(scope);
            TransactionalFilePair.recover(storePath, runtimePath);
            writeScopeMetadata(scope);
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not prepare Quest Ledger scope", exception);
            unload();
            return;
        }

        QuestFile loadedFile = new QuestFile(List.of());
        if (Files.exists(storePath)) {
            try {
                String loadedSource = Files.readString(storePath, StandardCharsets.UTF_8);
                QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(loadedSource);
                if (!parsed.validation().valid()) {
                    Diagnostic diagnostic = parsed.validation().diagnostics().getFirst();
                    QuestLedger.LOGGER.error(
                            "Could not load scoped Quest Ledger store: {}",
                            invalidResult(diagnostic).message()
                    );
                } else {
                    loadedFile = parsed.file();
                }
            } catch (IOException | QuestScriptException exception) {
                QuestLedger.LOGGER.error("Could not read scoped Quest Ledger store", exception);
            }
        }

        List<RuntimeEntry> loadedRuntime = readRuntimeEntries();
        List<RuntimeEntry> reconciled = reconcileRuntime(
                loadedFile,
                loadedRuntime,
                player
        );
        cache(loadedFile, reconciled);
        if (!reconciled.equals(loadedRuntime)) {
            persistRuntimeOnly();
        }

        QuestLedger.LOGGER.info(
                "Loaded {} Quest Ledger quest(s) for {} scope '{}'",
                cached.quests().size(),
                scope.kind(),
                scope.displayName()
        );
    }

    private static void unload() {
        currentScope = null;
        storeDirectory = null;
        storePath = null;
        runtimePath = null;
        historyPath = null;
        cached = new QuestFile(List.of());
        runtimeEntries = List.of();
        source = "";
        changedAtMillis = Util.getMillis();
    }

    public static synchronized SaveResult append(String questSource) {
        if (currentScope == null) {
            return new SaveResult(false, "Join a world or server before saving a quest.");
        }

        try {
            QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(questSource);
            if (!parsed.validation().valid()) {
                return invalidResult(parsed.validation().diagnostics().getFirst());
            }

            List<QuestDefinition> merged = new ArrayList<>(cached.quests());
            merged.addAll(parsed.file().quests());

            List<RuntimeEntry> mergedRuntime = new ArrayList<>(runtimeEntries);
            LocalPlayer player = Minecraft.getInstance().player;
            for (QuestDefinition quest : parsed.file().quests()) {
                mergedRuntime.add(captureRuntimeEntry(quest, player));
            }
            return persist(new QuestFile(merged), mergedRuntime);
        } catch (QuestScriptException exception) {
            return new SaveResult(false, exception.getMessage());
        }
    }

    public static synchronized SaveResult replace(String completeSource) {
        if (currentScope == null) {
            return new SaveResult(false, "Join a world or server before saving quests.");
        }

        try {
            QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(completeSource);
            if (!parsed.validation().valid()) {
                return invalidResult(parsed.validation().diagnostics().getFirst());
            }
            List<RuntimeEntry> reconciled = reconcileRuntime(
                    parsed.file(),
                    runtimeEntries,
                    Minecraft.getInstance().player
            );
            return persist(parsed.file(), reconciled);
        } catch (QuestScriptException exception) {
            return new SaveResult(false, exception.getMessage());
        }
    }

    public static synchronized SaveResult complete(QuestDefinition completedQuest) {
        int index = questIndex(completedQuest);
        if (index < 0) {
            return new SaveResult(false, "Quest was no longer present in the active store.");
        }

        List<QuestDefinition> remaining = new ArrayList<>(cached.quests());
        remaining.remove(index);
        List<RuntimeEntry> remainingRuntime = new ArrayList<>(runtimeEntries);
        if (index < remainingRuntime.size()) {
            remainingRuntime.remove(index);
        }

        SaveResult save = persist(new QuestFile(remaining), remainingRuntime);
        if (!save.success()) {
            return save;
        }

        appendHistory(completedQuest);
        return new SaveResult(true, "Completed quest: " + completedQuest.title());
    }

    public static synchronized long statisticDelta(
            QuestDefinition quest,
            StatReference reference,
            long currentValue
    ) {
        int index = questIndex(quest);
        if (index < 0 || index >= runtimeEntries.size()) {
            return 0L;
        }

        RuntimeEntry entry = runtimeEntries.get(index);
        Long baseline = entry.baselines().get(reference);
        if (baseline == null || currentValue < baseline) {
            Map<StatReference, Long> updatedBaselines = new HashMap<>(entry.baselines());
            updatedBaselines.put(reference, currentValue);
            List<RuntimeEntry> updatedEntries = new ArrayList<>(runtimeEntries);
            updatedEntries.set(index, entry.withBaselines(updatedBaselines));
            runtimeEntries = List.copyOf(updatedEntries);
            persistRuntimeOnly();
            return 0L;
        }
        return currentValue - baseline;
    }

    static synchronized String runtimeKey(QuestDefinition quest) {
        int index = questIndex(quest);
        if (index >= 0 && index < runtimeEntries.size()) {
            return "runtime:" + runtimeEntries.get(index).instanceId();
        }
        return quest.id().map(id -> "id:" + id)
                .orElseGet(() -> "unresolved:" + fingerprint(quest));
    }

    private static SaveResult persist(QuestFile file, List<RuntimeEntry> entries) {
        String canonical = new QuestScriptFormatter().format(file);
        List<RuntimeEntry> committedEntries = List.copyOf(entries);
        try {
            Files.createDirectories(storeDirectory);
            TransactionalFilePair.commit(
                    storePath,
                    canonical,
                    runtimePath,
                    runtimeSource(committedEntries)
            );
            cached = file;
            source = canonical;
            runtimeEntries = committedEntries;
            changedAtMillis = Util.getMillis();
            return new SaveResult(true, "Saved " + file.quests().size() + " quest(s).");
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not save scoped Quest Ledger store", exception);
            return new SaveResult(false, exception.getMessage());
        }
    }

    private static void cache(QuestFile file, List<RuntimeEntry> entries) {
        cached = file;
        runtimeEntries = List.copyOf(entries);
        source = new QuestScriptFormatter().format(file);
        changedAtMillis = Util.getMillis();
    }

    private static List<RuntimeEntry> reconcileRuntime(
            QuestFile file,
            List<RuntimeEntry> existing,
            LocalPlayer player
    ) {
        Map<String, Deque<RuntimeEntry>> byFingerprint = new HashMap<>();
        for (RuntimeEntry entry : existing) {
            byFingerprint.computeIfAbsent(entry.fingerprint(), ignored -> new ArrayDeque<>())
                    .addLast(entry);
        }

        List<RuntimeEntry> reconciled = new ArrayList<>();
        for (QuestDefinition quest : file.quests()) {
            String fingerprint = fingerprint(quest);
            Deque<RuntimeEntry> candidates = byFingerprint.get(fingerprint);
            if (candidates != null && !candidates.isEmpty()) {
                reconciled.add(candidates.removeFirst());
            } else {
                reconciled.add(captureRuntimeEntry(quest, player));
            }
        }
        return List.copyOf(reconciled);
    }

    private static RuntimeEntry captureRuntimeEntry(
            QuestDefinition quest,
            LocalPlayer player
    ) {
        Map<StatReference, Long> baselines = new HashMap<>();
        if (player != null) {
            for (StatReference reference : QuestStatisticAccess.collect(
                    quest.completionCondition()
            )) {
                QuestStatisticAccess.read(player, reference)
                        .ifPresent(value -> baselines.put(reference, value));
            }
        }
        return new RuntimeEntry(
                UUID.randomUUID().toString(),
                fingerprint(quest),
                Instant.now().toEpochMilli(),
                baselines
        );
    }

    private static String fingerprint(QuestDefinition quest) {
        String canonical = new QuestScriptFormatter().format(new QuestFile(List.of(quest)));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static List<RuntimeEntry> readRuntimeEntries() {
        if (runtimePath == null || !Files.exists(runtimePath)) {
            return List.of();
        }

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(Files.readString(
                    runtimePath,
                    StandardCharsets.UTF_8
            )));
            int count = Integer.parseInt(properties.getProperty("entry.count", "0"));
            List<RuntimeEntry> entries = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                String prefix = "entry." + index + ".";
                String instanceId = properties.getProperty(prefix + "instance_id", "");
                if (instanceId.isBlank()) {
                    instanceId = UUID.randomUUID().toString();
                }
                String fingerprint = properties.getProperty(prefix + "fingerprint", "");
                long createdAt = Long.parseLong(
                        properties.getProperty(prefix + "created_at", "0")
                );
                Map<StatReference, Long> baselines = new HashMap<>();
                String baselinePrefix = prefix + "baseline.";
                for (String name : properties.stringPropertyNames()) {
                    if (!name.startsWith(baselinePrefix)) {
                        continue;
                    }
                    decodeReference(name.substring(baselinePrefix.length()))
                            .ifPresent(reference -> baselines.put(
                                    reference,
                                    Long.parseLong(properties.getProperty(name))
                            ));
                }
                if (!fingerprint.isBlank()) {
                    entries.add(new RuntimeEntry(instanceId, fingerprint, createdAt, baselines));
                }
            }
            return List.copyOf(entries);
        } catch (IOException | IllegalArgumentException exception) {
            QuestLedger.LOGGER.error("Could not load Quest Ledger runtime state", exception);
            return List.of();
        }
    }

    private static void persistRuntimeOnly() {
        try {
            persistRuntimeOnlyOrThrow();
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not save Quest Ledger runtime state", exception);
        }
    }

    private static void persistRuntimeOnlyOrThrow() throws IOException {
        if (runtimePath == null) {
            return;
        }
        writeAtomically(runtimePath, runtimeSource(runtimeEntries));
    }

    private static String runtimeSource(List<RuntimeEntry> entries) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", "2");
        properties.setProperty("entry.count", Integer.toString(entries.size()));
        for (int index = 0; index < entries.size(); index++) {
            RuntimeEntry entry = entries.get(index);
            String prefix = "entry." + index + ".";
            properties.setProperty(prefix + "instance_id", entry.instanceId());
            properties.setProperty(prefix + "fingerprint", entry.fingerprint());
            properties.setProperty(prefix + "created_at", Long.toString(entry.createdAtMillis()));
            for (Map.Entry<StatReference, Long> baseline : entry.baselines().entrySet()) {
                properties.setProperty(
                        prefix + "baseline." + encodeReference(baseline.getKey()),
                        Long.toString(baseline.getValue())
                );
            }
        }
        StringWriter writer = new StringWriter();
        properties.store(writer, "Quest Ledger per-quest runtime state");
        return writer.toString();
    }

    private static String encodeReference(StatReference reference) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(reference.externalForm().getBytes(StandardCharsets.UTF_8));
    }

    private static Optional<StatReference> decodeReference(String encoded) {
        try {
            String external = new String(
                    Base64.getUrlDecoder().decode(encoded),
                    StandardCharsets.UTF_8
            );
            return StatReference.parseExternalForm(external);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void writeScopeMetadata(QuestWorldScope scope) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("scope.key", scope.key());
        properties.setProperty("scope.display_name", scope.displayName());
        properties.setProperty("scope.kind", scope.kind().name());
        StringWriter writer = new StringWriter();
        properties.store(writer, "Quest Ledger world/server scope");
        writeAtomically(storeDirectory.resolve("scope.properties"), writer.toString());
    }

    private static void migrateLegacyStore(QuestWorldScope scope) throws IOException {
        if (Files.exists(LEGACY_MIGRATION_MARKER) || !Files.exists(LEGACY_STORE_PATH)) {
            return;
        }

        Files.createDirectories(storeDirectory);
        if (!Files.exists(storePath)) {
            Files.copy(LEGACY_STORE_PATH, storePath);
        }
        if (Files.exists(LEGACY_HISTORY_PATH) && !Files.exists(historyPath)) {
            Files.copy(LEGACY_HISTORY_PATH, historyPath);
        }

        Path archiveDirectory = BASE_DIRECTORY.resolve("legacy");
        Files.createDirectories(archiveDirectory);
        moveReplacing(LEGACY_STORE_PATH, archiveDirectory.resolve("quests.qs"));
        if (Files.exists(LEGACY_HISTORY_PATH)) {
            moveReplacing(
                    LEGACY_HISTORY_PATH,
                    archiveDirectory.resolve("completed-history.log")
            );
        }

        Properties marker = new Properties();
        marker.setProperty("migrated_at", Instant.now().toString());
        marker.setProperty("scope.key", scope.key());
        marker.setProperty("scope.display_name", scope.displayName());
        StringWriter writer = new StringWriter();
        marker.store(writer, "Quest Ledger legacy client-wide store migration");
        writeAtomically(LEGACY_MIGRATION_MARKER, writer.toString());

        QuestLedger.LOGGER.info(
                "Migrated legacy client-wide quests into scope '{}'",
                scope.displayName()
        );
    }

    private static void moveReplacing(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(
                    sourcePath,
                    targetPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
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

    private static void appendHistory(QuestDefinition quest) {
        if (historyPath == null) {
            return;
        }
        String id = quest.id().orElse("-");
        String line = Instant.now() + "\t" + escapeHistory(id) + "\t"
                + escapeHistory(quest.title()) + System.lineSeparator();
        try {
            Files.createDirectories(storeDirectory);
            Files.writeString(
                    historyPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            QuestLedger.LOGGER.warn(
                    "Completed quest was removed, but scoped history could not be written",
                    exception
            );
        }
    }

    private static int questIndex(QuestDefinition quest) {
        for (int index = 0; index < cached.quests().size(); index++) {
            if (cached.quests().get(index) == quest) {
                return index;
            }
        }
        return cached.quests().indexOf(quest);
    }

    private static String escapeHistory(String value) {
        return value.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static SaveResult invalidResult(Diagnostic diagnostic) {
        return new SaveResult(false, diagnostic.code() + ": " + diagnostic.message()
                + " (" + diagnostic.location().line() + ":"
                + diagnostic.location().column() + ")");
    }

    public static synchronized QuestFile snapshot() {
        return cached;
    }

    public static synchronized String source() {
        return source;
    }

    public static synchronized long changedAtMillis() {
        return changedAtMillis;
    }

    public static synchronized Optional<QuestWorldScope> activeScope() {
        return Optional.ofNullable(currentScope);
    }

    private record RuntimeEntry(
            String instanceId,
            String fingerprint,
            long createdAtMillis,
            Map<StatReference, Long> baselines
    ) {
        private RuntimeEntry {
            instanceId = instanceId == null || instanceId.isBlank()
                    ? UUID.randomUUID().toString()
                    : instanceId;
            baselines = Map.copyOf(baselines);
        }

        private RuntimeEntry withBaselines(Map<StatReference, Long> updated) {
            return new RuntimeEntry(instanceId, fingerprint, createdAtMillis, updated);
        }
    }

    public record SaveResult(boolean success, String message) {
    }
}
