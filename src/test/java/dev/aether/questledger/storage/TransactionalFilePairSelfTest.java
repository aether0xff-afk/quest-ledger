package dev.aether.questledger.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TransactionalFilePairSelfTest {
    public static void main(String[] args) throws Exception {
        commitsBothFilesTogether();
        rollsBackCheckedFailure();
        recoversAbruptInterruption();
        supportsFirstSaveWithoutExistingFiles();
        System.out.println("Transactional store self-test passed (4 scenarios).");
    }

    private static void commitsBothFilesTogether() throws Exception {
        Path directory = Files.createTempDirectory("quest-ledger-commit-");
        Path quests = directory.resolve("quests.qs");
        Path runtime = directory.resolve("runtime-state.properties");
        write(quests, "old quests");
        write(runtime, "old runtime");

        TransactionalFilePair.commit(quests, "new quests", runtime, "new runtime");

        require(read(quests).equals("new quests"), "Quest source was not committed");
        require(read(runtime).equals("new runtime"), "Runtime state was not committed");
        requireNoArtifacts(directory);
    }

    private static void rollsBackCheckedFailure() throws Exception {
        Path directory = Files.createTempDirectory("quest-ledger-failure-");
        Path quests = directory.resolve("quests.qs");
        Path runtime = directory.resolve("runtime-state.properties");
        write(quests, "old quests");
        write(runtime, "old runtime");

        boolean failed = false;
        try {
            TransactionalFilePair.commit(
                    quests,
                    "new quests",
                    runtime,
                    "new runtime",
                    step -> {
                        if (step == TransactionalFilePair.Step.FIRST_INSTALLED) {
                            throw new IOException("Injected move failure");
                        }
                    }
            );
        } catch (IOException expected) {
            failed = true;
        }

        require(failed, "Injected failure did not escape commit");
        require(read(quests).equals("old quests"), "Quest source was not rolled back");
        require(read(runtime).equals("old runtime"), "Runtime state was not rolled back");
        requireNoArtifacts(directory);
    }

    private static void recoversAbruptInterruption() throws Exception {
        Path directory = Files.createTempDirectory("quest-ledger-recovery-");
        Path quests = directory.resolve("quests.qs");
        Path runtime = directory.resolve("runtime-state.properties");
        write(quests, "old quests");
        write(runtime, "old runtime");

        boolean interrupted = false;
        try {
            TransactionalFilePair.commit(
                    quests,
                    "new quests",
                    runtime,
                    "new runtime",
                    step -> {
                        if (step == TransactionalFilePair.Step.FIRST_INSTALLED) {
                            throw new SimulatedProcessTermination();
                        }
                    }
            );
        } catch (SimulatedProcessTermination expected) {
            interrupted = true;
        }

        require(interrupted, "Abrupt interruption was not simulated");
        TransactionalFilePair.recover(quests, runtime);
        require(read(quests).equals("old quests"), "Recovery kept a mixed quest generation");
        require(read(runtime).equals("old runtime"), "Recovery kept a mixed runtime generation");
        requireNoArtifacts(directory);
    }

    private static void supportsFirstSaveWithoutExistingFiles() throws Exception {
        Path directory = Files.createTempDirectory("quest-ledger-first-save-");
        Path quests = directory.resolve("quests.qs");
        Path runtime = directory.resolve("runtime-state.properties");

        TransactionalFilePair.commit(quests, "first quests", runtime, "first runtime");

        require(read(quests).equals("first quests"), "First quest save failed");
        require(read(runtime).equals("first runtime"), "First runtime save failed");
        requireNoArtifacts(directory);
    }

    private static void requireNoArtifacts(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            boolean artifact = files.anyMatch(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".next")
                        || name.endsWith(".bak")
                        || name.contains("transaction");
            });
            require(!artifact, "Transaction artifact was left behind");
        }
    }

    private static void write(Path path, String value) throws IOException {
        Files.writeString(path, value, StandardCharsets.UTF_8);
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class SimulatedProcessTermination extends RuntimeException {
    }
}
