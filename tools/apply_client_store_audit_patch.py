from pathlib import Path

path = Path("src/client/java/dev/aether/questledger/client/ClientQuestStore.java")
source = path.read_text(encoding="utf-8")

source = source.replace(
    "import dev.aether.questledger.questscript.validation.Diagnostic;\n",
    "import dev.aether.questledger.questscript.validation.Diagnostic;\n"
    "import dev.aether.questledger.questscript.validation.QuestRuntimeSupportValidator;\n"
    "import dev.aether.questledger.questscript.validation.QuestScriptValidator;\n"
    "import dev.aether.questledger.questscript.validation.ValidationResult;\n",
)

old_append = """            List<QuestDefinition> merged = new ArrayList<>(cached.quests());
            merged.addAll(parsed.file().quests());

            List<RuntimeEntry> mergedRuntime = new ArrayList<>(runtimeEntries);"""
new_append = """            List<QuestDefinition> merged = new ArrayList<>(cached.quests());
            merged.addAll(parsed.file().quests());
            QuestFile mergedFile = new QuestFile(merged);
            Optional<SaveResult> validationFailure = validateForSave(mergedFile);
            if (validationFailure.isPresent()) {
                return validationFailure.get();
            }

            List<RuntimeEntry> mergedRuntime = new ArrayList<>(runtimeEntries);"""
if old_append not in source:
    raise SystemExit("append merge block was not found")
source = source.replace(old_append, new_append)
source = source.replace(
    "            return persist(new QuestFile(merged), mergedRuntime);",
    "            return persist(mergedFile, mergedRuntime);",
)

old_replace = """            List<RuntimeEntry> reconciled = reconcileRuntime(
                    parsed.file(),
                    runtimeEntries,
                    Minecraft.getInstance().player
            );
            return persist(parsed.file(), reconciled);"""
new_replace = """            Optional<SaveResult> validationFailure = validateForSave(parsed.file());
            if (validationFailure.isPresent()) {
                return validationFailure.get();
            }
            List<RuntimeEntry> reconciled = reconcileRuntime(
                    parsed.file(),
                    runtimeEntries,
                    Minecraft.getInstance().player
            );
            return persist(parsed.file(), reconciled);"""
if old_replace not in source:
    raise SystemExit("replace validation block was not found")
source = source.replace(old_replace, new_replace)

old_invalid = """    private static SaveResult invalidResult(Diagnostic diagnostic) {
        return new SaveResult(false, diagnostic.code() + ": " + diagnostic.message()
                + " (" + diagnostic.location().line() + ":"
                + diagnostic.location().column() + ")");
    }
"""
new_invalid = """    private static Optional<SaveResult> validateForSave(QuestFile file) {
        ValidationResult semantic = new QuestScriptValidator().validate(file);
        if (!semantic.valid()) {
            return Optional.of(invalidResult(semantic.diagnostics().getFirst()));
        }
        ValidationResult runtime = new QuestRuntimeSupportValidator().validate(file);
        if (!runtime.valid()) {
            return Optional.of(invalidResult(runtime.diagnostics().getFirst()));
        }
        return Optional.empty();
    }

    private static SaveResult invalidResult(Diagnostic diagnostic) {
        return new SaveResult(false, diagnostic.code() + ": " + diagnostic.message()
                + " (" + diagnostic.location().line() + ":"
                + diagnostic.location().column() + ")");
    }
"""
if old_invalid not in source:
    raise SystemExit("invalid-result block was not found")
source = source.replace(old_invalid, new_invalid)

path.write_text(source, encoding="utf-8")
