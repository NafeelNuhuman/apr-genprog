package de.uni_passau.apr.core.workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class WorkspaceCleaner {

    public WorkspaceCleaner() {
    }

    public static void deleteRecursively(Path root) throws IOException {
        // To clean the workspace directory recursively
        if (root == null) return;
        if (!Files.exists(root)) return;

        if (!root.getFileName().toString().startsWith("apr-")) {
            throw new IllegalArgumentException("The dir does not start with 'apr-': " + root);
        }
        Path tmp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path rootAbsPath = root.toAbsolutePath().normalize();

        if (!rootAbsPath.startsWith(tmp)) {
            throw new IllegalArgumentException("The dir is outside the temp dir: " + root);
        }

        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
