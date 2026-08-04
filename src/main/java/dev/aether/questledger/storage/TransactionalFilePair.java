package dev.aether.questledger.storage;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

/**
 * Commits two UTF-8 files as one recoverable transaction.
 *
 * <p>The two files must share a directory. New contents are staged before either live file
 * is changed. A small journal and backups allow the next startup to roll back an interrupted
 * commit, so readers never intentionally accept a mixed generation.</p>
 */
public final class TransactionalFilePair {
    private static final String JOURNAL_NAME = ".quest-ledger-store.transaction";
    private static final String PHASE_PREPARED = "prepared";
    private static final String PHASE_COMMITTED = "committed";

    private TransactionalFilePair() {
    }

    public static void commit(
            Path firstPath,
            String firstContent,
            Path secondPath,
            String secondContent
    ) throws IOException {
        commit(firstPath, firstContent, secondPath, secondContent, ignored -> { });
    }

    static void commit(
            Path firstPath,
            String firstContent,
            Path secondPath,
            String secondContent,
            CommitHook hook
    ) throws IOException {
        Paths paths = Paths.resolve(firstPath, secondPath);
        Objects.requireNonNull(firstContent, "firstContent");
        Objects.requireNonNull(secondContent, "secondContent");
        Objects.requireNonNull(hook, "hook");

        Files.createDirectories(paths.directory());
        recover(firstPath, secondPath);
        deleteIfExists(paths.firstNext());
        deleteIfExists(paths.secondNext());
        deleteIfExists(paths.firstBackup());
        deleteIfExists(paths.secondBackup());

        Files.writeString(paths.firstNext(), firstContent, StandardCharsets.UTF_8);
        Files.writeString(paths.secondNext(), secondContent, StandardCharsets.UTF_8);

        boolean firstExisted = Files.exists(firstPath);
        boolean secondExisted = Files.exists(secondPath);
        writeJournal(paths.journal(), PHASE_PREPARED, firstExisted, secondExisted);
        hook.after(Step.JOURNAL_WRITTEN);

        try {
            if (firstExisted) {
                moveReplacing(firstPath, paths.firstBackup());
            }
            hook.after(Step.FIRST_BACKED_UP);

            if (secondExisted) {
                moveReplacing(secondPath, paths.secondBackup());
            }
            hook.after(Step.SECOND_BACKED_UP);

            moveReplacing(paths.firstNext(), firstPath);
            hook.after(Step.FIRST_INSTALLED);

            moveReplacing(paths.secondNext(), secondPath);
            hook.after(Step.SECOND_INSTALLED);

            writeJournal(paths.journal(), PHASE_COMMITTED, firstExisted, secondExisted);
        } catch (IOException exception) {
            try {
                recover(firstPath, secondPath);
            } catch (IOException recoveryException) {
                exception.addSuppressed(recoveryException);
            }
            throw exception;
        }

        cleanupCommittedArtifacts(paths);
    }

    /**
     * Recovers an interrupted transaction. An uncommitted journal is rolled back; a committed
     * journal only needs artifact cleanup.
     */
    public static void recover(Path firstPath, Path secondPath) throws IOException {
        Paths paths = Paths.resolve(firstPath, secondPath);
        if (!Files.exists(paths.journal())) {
            deleteIfExists(paths.firstNext());
            deleteIfExists(paths.secondNext());
            return;
        }

        Properties journal = readJournal(paths.journal());
        String phase = journal.getProperty("phase", "");
        boolean firstExisted = Boolean.parseBoolean(journal.getProperty("first_existed", "false"));
        boolean secondExisted = Boolean.parseBoolean(journal.getProperty("second_existed", "false"));

        if (PHASE_COMMITTED.equals(phase)) {
            if (!Files.exists(firstPath) || !Files.exists(secondPath)) {
                throw new IOException("Committed Quest Ledger transaction is missing a live file.");
            }
            cleanupCommittedArtifacts(paths);
            return;
        }

        restoreOriginal(firstPath, paths.firstBackup(), firstExisted);
        restoreOriginal(secondPath, paths.secondBackup(), secondExisted);
        cleanupArtifacts(paths);
    }

    private static void restoreOriginal(Path live, Path backup, boolean originallyExisted)
            throws IOException {
        if (Files.exists(backup)) {
            moveReplacing(backup, live);
            return;
        }
        if (!originallyExisted) {
            deleteIfExists(live);
        }
    }

    private static Properties readJournal(Path journalPath) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(Files.readString(journalPath, StandardCharsets.UTF_8)));
        return properties;
    }

    private static void writeJournal(
            Path journalPath,
            String phase,
            boolean firstExisted,
            boolean secondExisted
    ) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("phase", phase);
        properties.setProperty("first_existed", Boolean.toString(firstExisted));
        properties.setProperty("second_existed", Boolean.toString(secondExisted));
        StringWriter writer = new StringWriter();
        properties.store(writer, "Quest Ledger two-file transaction");
        writeAtomically(journalPath, writer.toString());
    }

    private static void cleanupCommittedArtifacts(Paths paths) {
        try {
            cleanupArtifacts(paths);
        } catch (IOException ignored) {
            // The committed live files are already valid. A later recovery or save retries cleanup.
        }
    }

    private static void cleanupArtifacts(Paths paths) throws IOException {
        IOException failure = null;
        for (Path path : new Path[] {
                paths.firstNext(),
                paths.secondNext(),
                paths.firstBackup(),
                paths.secondBackup(),
                paths.journal()
        }) {
            try {
                deleteIfExists(path);
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void writeAtomically(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        moveReplacing(temporary, path);
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    enum Step {
        JOURNAL_WRITTEN,
        FIRST_BACKED_UP,
        SECOND_BACKED_UP,
        FIRST_INSTALLED,
        SECOND_INSTALLED
    }

    @FunctionalInterface
    interface CommitHook {
        void after(Step step) throws IOException;
    }

    private record Paths(
            Path directory,
            Path journal,
            Path firstNext,
            Path secondNext,
            Path firstBackup,
            Path secondBackup
    ) {
        private static Paths resolve(Path firstPath, Path secondPath) {
            Objects.requireNonNull(firstPath, "firstPath");
            Objects.requireNonNull(secondPath, "secondPath");
            Path firstAbsolute = firstPath.toAbsolutePath().normalize();
            Path secondAbsolute = secondPath.toAbsolutePath().normalize();
            Path directory = firstAbsolute.getParent();
            if (directory == null || !directory.equals(secondAbsolute.getParent())) {
                throw new IllegalArgumentException("Transactional files must share a directory.");
            }
            if (firstAbsolute.equals(secondAbsolute)) {
                throw new IllegalArgumentException("Transactional files must be different paths.");
            }
            return new Paths(
                    directory,
                    directory.resolve(JOURNAL_NAME),
                    firstAbsolute.resolveSibling(firstAbsolute.getFileName() + ".next"),
                    secondAbsolute.resolveSibling(secondAbsolute.getFileName() + ".next"),
                    firstAbsolute.resolveSibling(firstAbsolute.getFileName() + ".bak"),
                    secondAbsolute.resolveSibling(secondAbsolute.getFileName() + ".bak")
            );
        }
    }
}
