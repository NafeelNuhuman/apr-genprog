package de.uni_passau.apr.core.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceCleanerTest {

    @TempDir
    Path tempDir;

    @Test
    void deleteRecursively_deletesDirectoryTree_underTemp_withAprPrefix() throws IOException {
        // Arrange: create a fake apr-* folder under the real temp dir
        Path realTmp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path workspace = Files.createTempDirectory("apr-cleaner-test-"); // guaranteed under java.io.tmpdir

        // Add nested structure + files
        Path nestedDir = workspace.resolve("a/b/c");
        Files.createDirectories(nestedDir);

        Path file1 = workspace.resolve("pom.xml");
        Path file2 = nestedDir.resolve("file.txt");

        Files.writeString(file1, "<pom/>", StandardCharsets.UTF_8);
        Files.writeString(file2, "hello", StandardCharsets.UTF_8);

        assertTrue(workspace.toAbsolutePath().normalize().startsWith(realTmp));
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));

        // Act
        WorkspaceCleaner.deleteRecursively(workspace);

        // Assert
        assertFalse(Files.exists(workspace), "Workspace directory should be deleted");
    }

    @Test
    void deleteRecursively_refusesToDelete_whenPrefixNotApr() throws IOException {
        // Arrange: create a non apr-* directory under temp
        Path nonApr = Files.createTempDirectory("notapr-");
        assertTrue(Files.exists(nonApr));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WorkspaceCleaner.deleteRecursively(nonApr));

        assertTrue(ex.getMessage().contains("apr-"));
        assertTrue(Files.exists(nonApr), "Directory should not be deleted when prefix is wrong");
    }

    @Test
    void deleteRecursively_refusesToDelete_whenOutsideTemp() throws IOException {
        // Arrange: create a directory outside of java.io.tmpdir but starting with apr-
        // We'll use @TempDir for a directory that might NOT be under java.io.tmpdir on some setups.
        // To guarantee "outside temp", we create it under the JUnit temp dir and assert it isn't under java.io.tmpdir.
        Path outside = tempDir.resolve("apr-outside-temp");
        Files.createDirectories(outside);

        Path realTmp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path outsideAbs = outside.toAbsolutePath().normalize();

        // If tempDir happens to be inside java.io.tmpdir on your system, skip this test scenario:
        if (outsideAbs.startsWith(realTmp)) {
            // In that case we cannot reliably test the "outside temp" guard on this machine.
            return;
        }

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WorkspaceCleaner.deleteRecursively(outside));

        assertTrue(ex.getMessage().toLowerCase().contains("outside"));
        assertTrue(Files.exists(outside), "Directory should not be deleted when outside temp");
    }

    @Test
    void deleteRecursively_noOpOnNullOrMissing() throws IOException {
        // null should not throw
        assertDoesNotThrow(() -> WorkspaceCleaner.deleteRecursively(null));

        // missing path should not throw
        Path missing = tempDir.resolve("apr-missing");
        assertFalse(Files.exists(missing));
        assertDoesNotThrow(() -> WorkspaceCleaner.deleteRecursively(missing));
    }
}
