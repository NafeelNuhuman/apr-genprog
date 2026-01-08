package de.uni_passau.apr.core.workspace;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceBuilderTest {

    @TempDir
    Path tempDir;

    private Path createdWorkspace; // track to delete after each test

    @AfterEach
    void cleanup() throws IOException {
        if (createdWorkspace != null && Files.exists(createdWorkspace)) {
            deleteRecursively(createdWorkspace);
        }
    }

    @Test
    void build_createsWorkspaceWithPomAndSources() throws IOException {
        // Arrange: create a fake benchmark layout under tempDir
        Path benchmarkRoot = tempDir.resolve("benchmarks");
        Path bm1 = benchmarkRoot.resolve("bm1");
        Path buggyDir = bm1.resolve("buggy");
        Path testsDir = bm1.resolve("tests");

        Files.createDirectories(buggyDir);
        Files.createDirectories(testsDir);

        Path buggyProgramPath = buggyDir.resolve("Program.java");
        Path testSuitePath = testsDir.resolve("ProgramTest.java");

        String originalBuggy = """
                public class Program {
                    public static int add(int a, int b) { return a + b; }
                }
                """;
        String testSuite = """
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;

                public class ProgramTest {
                    @Test void adds() {
                        assertEquals(3, Program.add(1,2));
                    }
                }
                """;

        Files.writeString(buggyProgramPath, originalBuggy, StandardCharsets.UTF_8);
        Files.writeString(testSuitePath, testSuite, StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");
        cfg.setBuggyProgramPath(buggyProgramPath);
        cfg.setTestSuitePath(testSuitePath);

        String candidateVariant = """
                public class Program {
                    public static int add(int a, int b) { return a + b + 100; }
                }
                """;

        WorkspaceBuilder builder = new WorkspaceBuilder();

        // Act
        createdWorkspace = builder.build(cfg, candidateVariant);

        // Assert: workspace exists
        assertNotNull(createdWorkspace);
        assertTrue(Files.exists(createdWorkspace), "Workspace directory should exist");
        assertTrue(Files.isDirectory(createdWorkspace), "Workspace path should be a directory");

        // Assert: pom.xml exists
        Path pom = createdWorkspace.resolve("pom.xml");
        assertTrue(Files.exists(pom), "pom.xml should exist");

        String pomContent = Files.readString(pom, StandardCharsets.UTF_8);
        assertTrue(pomContent.contains("<artifactId>junit-jupiter</artifactId>"), "pom.xml should include JUnit Jupiter");
        assertTrue(pomContent.contains("<maven.compiler.release>17</maven.compiler.release>"),
                "pom.xml should enforce Java 17 compilation");

        // Assert: candidate program written to src/main/java with the expected filename
        Path writtenProgram = createdWorkspace.resolve("src/main/java/Program.java");
        assertTrue(Files.exists(writtenProgram), "Candidate Program.java should be created in src/main/java");
        String writtenProgramContent = Files.readString(writtenProgram, StandardCharsets.UTF_8);
        assertEquals(candidateVariant, writtenProgramContent, "Candidate program content should match exactly");

        // Assert: test suite copied to src/test/java with same filename
        Path copiedTest = createdWorkspace.resolve("src/test/java/ProgramTest.java");
        assertTrue(Files.exists(copiedTest), "Test suite should be copied to src/test/java");
        String copiedTestContent = Files.readString(copiedTest, StandardCharsets.UTF_8);
        assertEquals(testSuite, copiedTestContent, "Copied test suite should match original test suite content");
    }

    @Test
    void build_usesBenchmarkFileNamesForProgramAndTest() throws IOException {
        // Arrange: different filenames (still valid for your Option A discovery)
        Path buggyProgramPath = tempDir.resolve("SomeOtherName.java");
        Path testSuitePath = tempDir.resolve("WeirdTestName.java");

        Files.writeString(buggyProgramPath, "public class SomeOtherName {}", StandardCharsets.UTF_8);
        Files.writeString(testSuitePath, "public class WeirdTestName {}", StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bmX");
        cfg.setBuggyProgramPath(buggyProgramPath);
        cfg.setTestSuitePath(testSuitePath);

        String candidateVariant = "public class SomeOtherName {}";

        WorkspaceBuilder builder = new WorkspaceBuilder();

        // Act
        createdWorkspace = builder.build(cfg, candidateVariant);

        // Assert: files should be created/copied using those filenames
        assertTrue(Files.exists(createdWorkspace.resolve("src/main/java/SomeOtherName.java")));
        assertTrue(Files.exists(createdWorkspace.resolve("src/test/java/WeirdTestName.java")));
    }

    @Test
    void build_createsStandardMavenDirectories() throws IOException {
        // Arrange
        Path buggyProgramPath = tempDir.resolve("Program.java");
        Path testSuitePath = tempDir.resolve("ProgramTest.java");

        Files.writeString(buggyProgramPath, "public class Program {}", StandardCharsets.UTF_8);
        Files.writeString(testSuitePath, "public class ProgramTest {}", StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bmDirs");
        cfg.setBuggyProgramPath(buggyProgramPath);
        cfg.setTestSuitePath(testSuitePath);

        WorkspaceBuilder builder = new WorkspaceBuilder();

        // Act
        createdWorkspace = builder.build(cfg, "public class Program {}");

        // Assert
        assertTrue(Files.isDirectory(createdWorkspace.resolve("src/main/java")), "src/main/java should exist");
        assertTrue(Files.isDirectory(createdWorkspace.resolve("src/test/java")), "src/test/java should exist");
    }

    private static void deleteRecursively(Path root) throws IOException {
        // delete files before directories
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete " + path, e);
                        }
                    });
        }
    }
}
