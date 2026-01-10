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
        // Implementation to clean the workspace directory recursively
        if (root == null) return;
        if (!Files.exists(root)) return;

        if (!root.getFileName().toString().startsWith("apr-")) {
            throw new IllegalArgumentException("Refusing to delete directory not starting with 'apr-': " + root);
        }
        Path tmp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path rootAbs = root.toAbsolutePath().normalize();

        if (!rootAbs.startsWith(tmp)) {
            throw new IllegalArgumentException("Refusing to delete directory outside of temp dir: " + root);
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
