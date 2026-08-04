package dev.aether.questledger.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TransactionalFilePairSelfTest {
    private static int scenarios;

    public static void main(String[] args) throws Exception {
        commitsBothFilesTogether();
        supportsFirstSaveWithoutExistingFiles();
        rollsBackCheckedFailureAtEveryMutationStep();
        recoversAbruptInterruptionAtEveryMutationStep();
        recoversInterruptedFirstSaveAtEveryMutationStep();
        rejectsInvalidPathPairs();
        System.out.println("Transactional store self-test passed (" + scenarios
                + " scenarios across every transaction step).");
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
        scenarios++;
    }

    private static void supportsFirstSaveWithoutExistingFiles() throws Exception {
        Path directory = Files.createTempDirectory("quest-ledger-first-save-");
        Path quests = directory.resolve("quests.qs");
        Path runtime = directory.resolve("runtime-state.properties");

        TransactionalFilePair.commit(quests, "first quests", runtime, "first runtime");

        require(read(quests).equals("first quests"), "First quest save failed");
        require(read(runtime).equals("first runtime"), "First runtime save failed");
        requireNoArtifacts(directory);
        scenarios++;
    }

    private static void rollsBackCheckedFailureAtEveryMutationStep() throws Exception {
        for (TransactionalFilePair.Step failureStep : TransactionalFilePair.Step.values()) {
            Path directory = Files.createTempDirectory("quest-ledger-checked-" + failureStep + "-");
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
                            if (step == failureStep) {
                                throw new IOException("Injected failure at " + step);
                            }
                        }
                );
            } catch (IOException expected) {
                failed = true;
            }

            require(failed, "Checked failure did not escape at " + failureStep);
            TransactionalFilePair.recover(quests, runtime);
            require(read(quests).equals("old quests"),
                    "Checked failure kept new quest generation at " + failureStep);
            require(read(runtime).equals("old runtime"),
                    "Checked failure kept new runtime generation at " + failureStep);
            requireNoArtifacts(directory);
            scenarios++;
        }
    }

    private static void recoversAbruptInterruptionAtEveryMutationStep() throws Exception {
        for (TransactionalFilePair.Step failureStep : TransactionalFilePair.Step.values()) {
            Path directory = Files.createTempDirectory("quest-ledger-abrupt-" + failureStep + "-");
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
                            if (step == failureStep) {
                                throw new SimulatedProcessTermination();
                            }
                        }
                );
            } catch (SimulatedProcessTermination expected) {
                interrupted = true;
            }

            require(interrupted, "Abrupt interruption was not simulated at " + failureStep);
            TransactionalFilePair.recover(quests, runtime);
            require(read(quests).equals("old quests"),
                    "Abrupt recovery kept mixed quest generation at " + failureStep);
            require(read(runtime).equals("old runtime"),
                    "Abrupt recovery kept mixed runtime generation at " + failureStep);
            requireNoArtifacts(directory);
            scenarios++;
        }
    }

    private static void recoversInterruptedFirstSaveAtEveryMutationStep() throws Exception {
        for (TransactionalFilePair.Step failureStep : TransactionalFilePair.Step.values()) {
            Path directory = Files.createTempDirectory("quest-ledger-first-abrupt-"
                    + failureStep + "-");
            Path quests = directory.resolve("quests.qs");
            Path runtime = directory.resolve("runtime-state.properties");

            try {
                TransactionalFilePair.commit(
                        quests,
                        "new quests",
                        runtime,
                        "new runtime",
                        step -> {
                            if (step == failureStep) {
                                throw new SimulatedProcessTermination();
                            }
                        }
                );
                throw new AssertionError("First-save interruption did not occur at " + failureStep);
            } catch (SimulatedProcessTermination expected) {
                // Simulate the next process start.
            }

            TransactionalFilePair.recover(quests, runtime);
            require(!Files.exists(quests),
                    "Interrupted first save left quest file at " + failureStep);
            require(!Files.exists(runtime),
                    "Interrupted first save left runtime file at " + failureStep);
            requireNoArtifacts(directory);
            scenarios++;
        }
    }

    private static void rejectsInvalidPathPairs() throws Exception {
        Path firstDirectory = Files.createTempDirectory("quest-ledger-invalid-a-");
        Path secondDirectory = Files.createTempDirectory("quest-ledger-invalid-b-");
        boolean differentDirectoriesRejected = false;
        try {
            TransactionalFilePair.commit(
                    firstDirectory.resolve("a"), "a",
                    secondDirectory.resolve("b"), "b"
            );
        } catch (IllegalArgumentException expected) {
            differentDirectoriesRejected = true;
        }
        require(differentDirectoriesRejected, "Different transaction directories were accepted");
        scenarios++;

        Path same = firstDirectory.resolve("same");
        boolean samePathRejected = false;
        try {
            TransactionalFilePair.commit(same, "a", same, "b");
        } catch (IllegalArgumentException expected) {
            samePathRejected = true;
        }
        require(samePathRejected, "The same path was accepted twice");
        scenarios++;
    }

    private static void requireNoArtifacts(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            boolean artifact = files.anyMatch(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".next")
                        || name.endsWith(".bak")
                        || name.contains("transaction");
            });
            require(!artifact, "Transaction artifact was left behind in " + directory);
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
