package de.uni_passau.apr.core.workspace;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void build_createsWorkspaceWithPom_candidateAndCopiedTests() throws Exception {
        // Arrange: create dummy benchmark files
        Path benchmarkRoot = tempDir.resolve("benchmarks");
        Files.createDirectories(benchmarkRoot);

        Path buggy = tempDir.resolve("Program.java");
        Path tests = tempDir.resolve("ProgramTest.java");

        Files.writeString(buggy, "public class Program {}", StandardCharsets.UTF_8);
        Files.writeString(tests, "public class ProgramTest {}", StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");
        cfg.setBenchmarkRoot(benchmarkRoot);
        cfg.setBuggyProgramPath(buggy);
        cfg.setTestSuitePath(tests);

        String candidateVariant = "public class Program { public static int x = 1; }";

        WorkspaceBuilder builder = new WorkspaceBuilder();

        // Act
        Path workspace = builder.build(cfg, candidateVariant);

        // Assert: workspace folder exists
        assertNotNull(workspace);
        assertTrue(Files.exists(workspace));
        assertTrue(Files.isDirectory(workspace));
        assertTrue(workspace.getFileName().toString().startsWith("apr-bm1-"));

        // pom.xml exists + contains key markers
        Path pom = workspace.resolve("pom.xml");
        assertTrue(Files.exists(pom));
        String pomContent = Files.readString(pom, StandardCharsets.UTF_8);
        assertTrue(pomContent.contains("<artifactId>workspace</artifactId>"));
        assertTrue(pomContent.contains("maven-surefire-plugin"));
        assertTrue(pomContent.contains("junit-jupiter"));

        // candidate variant written to src/main/java/<buggy filename>
        Path mainJava = workspace.resolve("src/main/java").resolve(buggy.getFileName().toString());
        assertTrue(Files.exists(mainJava));
        assertEquals(candidateVariant, Files.readString(mainJava, StandardCharsets.UTF_8));

        // tests copied to src/test/java/<test filename>
        Path copiedTest = workspace.resolve("src/test/java").resolve(tests.getFileName().toString());
        assertTrue(Files.exists(copiedTest));
        assertEquals(Files.readString(tests, StandardCharsets.UTF_8),
                Files.readString(copiedTest, StandardCharsets.UTF_8));
    }

    @Test
    void build_replacesExistingTestFile() throws Exception {
        // Arrange
        Path benchmarkRoot = tempDir.resolve("benchmarks");
        Files.createDirectories(benchmarkRoot);

        Path buggy = tempDir.resolve("Program.java");
        Path tests = tempDir.resolve("ProgramTest.java");

        Files.writeString(buggy, "public class Program {}", StandardCharsets.UTF_8);
        Files.writeString(tests, "public class ProgramTest { /* original */ }", StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm2");
        cfg.setBenchmarkRoot(benchmarkRoot);
        cfg.setBuggyProgramPath(buggy);
        cfg.setTestSuitePath(tests);

        WorkspaceBuilder builder = new WorkspaceBuilder();
        Path workspace = builder.build(cfg, "public class Program {}");

        Path copiedTest = workspace.resolve("src/test/java").resolve("ProgramTest.java");
        assertTrue(Files.exists(copiedTest));

        // Make existing content different to ensure replacement happens
        Files.writeString(copiedTest, "public class ProgramTest { /* old workspace content */ }", StandardCharsets.UTF_8);
        assertNotEquals(Files.readString(tests, StandardCharsets.UTF_8),
                Files.readString(copiedTest, StandardCharsets.UTF_8));

        // Act: run build again; should overwrite existing copied test
        Path workspace2 = builder.build(cfg, "public class Program { public static int y = 2; }");

        // Assert: second build is a new temp directory (fresh workspace)
        assertNotEquals(workspace, workspace2);

        Path copiedTest2 = workspace2.resolve("src/test/java").resolve("ProgramTest.java");
        assertTrue(Files.exists(copiedTest2));
        assertEquals(Files.readString(tests, StandardCharsets.UTF_8),
                Files.readString(copiedTest2, StandardCharsets.UTF_8));
    }

    @Test
    void build_throwsWhenTestSuitePathMissing() throws Exception {
        Path benchmarkRoot = tempDir.resolve("benchmarks");
        Files.createDirectories(benchmarkRoot);

        Path buggy = tempDir.resolve("Program.java");
        Files.writeString(buggy, "public class Program {}", StandardCharsets.UTF_8);

        Path missingTests = tempDir.resolve("MissingTest.java");
        assertFalse(Files.exists(missingTests));

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm3");
        cfg.setBenchmarkRoot(benchmarkRoot);
        cfg.setBuggyProgramPath(buggy);
        cfg.setTestSuitePath(missingTests);

        WorkspaceBuilder builder = new WorkspaceBuilder();

        assertThrows(java.nio.file.NoSuchFileException.class,
                () -> builder.build(cfg, "public class Program {}"));
    }

    @Test
    void build_throwsWhenBuggyProgramPathIsNull() throws Exception {
        Path benchmarkRoot = tempDir.resolve("benchmarks");
        Files.createDirectories(benchmarkRoot);

        Path tests = tempDir.resolve("ProgramTest.java");
        Files.writeString(tests, "public class ProgramTest {}", StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm4");
        cfg.setBenchmarkRoot(benchmarkRoot);
        cfg.setBuggyProgramPath(null);
        cfg.setTestSuitePath(tests);

        WorkspaceBuilder builder = new WorkspaceBuilder();

        assertThrows(NullPointerException.class,
                () -> builder.build(cfg, "public class Program {}"));
    }
}
