package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import dev.aether.questledger.questscript.QuestScript;
import dev.aether.questledger.questscript.QuestScriptException;
import dev.aether.questledger.questscript.QuestScriptFormatter;
import dev.aether.questledger.questscript.ast.QuestDefinition;
import dev.aether.questledger.questscript.ast.QuestFile;
import dev.aether.questledger.questscript.validation.Diagnostic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClientQuestStore {
    private static final Path STORE_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("quest-ledger")
            .resolve("quests.qs");

    private static QuestFile cached = new QuestFile(List.of());
    private static String source = "";
    private static long changedAtMillis = Util.getMillis();

    private ClientQuestStore() {
    }

    public static synchronized void load() {
        if (!Files.exists(STORE_PATH)) {
            cached = new QuestFile(List.of());
            source = "";
            return;
        }

        try {
            String loaded = Files.readString(STORE_PATH, StandardCharsets.UTF_8);
            SaveResult result = parseAndCache(loaded, false);
            if (!result.success()) {
                QuestLedger.LOGGER.error("Could not load Quest Ledger store: {}", result.message());
            }
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not read Quest Ledger store", exception);
        }
    }

    public static synchronized SaveResult append(String questSource) {
        try {
            QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(questSource);
            if (!parsed.validation().valid()) {
                return invalidResult(parsed.validation().diagnostics().getFirst());
            }

            List<QuestDefinition> merged = new ArrayList<>(cached.quests());
            merged.addAll(parsed.file().quests());
            return persist(new QuestFile(merged));
        } catch (QuestScriptException exception) {
            return new SaveResult(false, exception.getMessage());
        }
    }

    public static synchronized SaveResult replace(String completeSource) {
        return parseAndCache(completeSource, true);
    }

    private static SaveResult parseAndCache(String completeSource, boolean persist) {
        try {
            QuestScript.ParsedQuestScript parsed = QuestScript.parseAndValidate(completeSource);
            if (!parsed.validation().valid()) {
                return invalidResult(parsed.validation().diagnostics().getFirst());
            }
            return persist ? persist(parsed.file()) : cache(parsed.file());
        } catch (QuestScriptException exception) {
            return new SaveResult(false, exception.getMessage());
        }
    }

    private static SaveResult persist(QuestFile file) {
        String canonical = new QuestScriptFormatter().format(file);
        try {
            Files.createDirectories(STORE_PATH.getParent());
            Files.writeString(STORE_PATH, canonical, StandardCharsets.UTF_8);
            cached = file;
            source = canonical;
            changedAtMillis = Util.getMillis();
            return new SaveResult(true, "Saved " + file.quests().size() + " quest(s).");
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not save Quest Ledger store", exception);
            return new SaveResult(false, exception.getMessage());
        }
    }

    private static SaveResult cache(QuestFile file) {
        cached = file;
        source = new QuestScriptFormatter().format(file);
        changedAtMillis = Util.getMillis();
        return new SaveResult(true, "Loaded " + file.quests().size() + " quest(s).");
    }

    private static SaveResult invalidResult(Diagnostic diagnostic) {
        return new SaveResult(false, diagnostic.code() + ": " + diagnostic.message()
                + " (" + diagnostic.location().line() + ":" + diagnostic.location().column() + ")");
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

    public record SaveResult(boolean success, String message) {
    }
}
