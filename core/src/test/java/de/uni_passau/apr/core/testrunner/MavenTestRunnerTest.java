package de.uni_passau.apr.core.testrunner;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.workspace.WorkspaceBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class MavenTestRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void runTests_returnsFailureForBuggyProgram_whenTestsFail() throws IOException {
        // Arrange benchmark files
        Path buggyProgram = tempDir.resolve("buggy").resolve("Program.java");
        Path tests = tempDir.resolve("tests").resolve("ProgramTest.java");
        Files.createDirectories(buggyProgram.getParent());
        Files.createDirectories(tests.getParent());

        // Buggy: add is wrong
        String buggySource = """
                public class Program {
                    public static int add(int a, int b) { return a + b + 1; }
                }
                """;
        String testSource = """
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;

                public class ProgramTest {
                    @Test void adds() {
                        assertEquals(3, Program.add(1,2));
                    }
                }
                """;
        Files.writeString(buggyProgram, buggySource, StandardCharsets.UTF_8);
        Files.writeString(tests, testSource, StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm-test");
        cfg.setBuggyProgramPath(buggyProgram);
        cfg.setTestSuitePath(tests);

        WorkspaceBuilder builder = new WorkspaceBuilder();
        Path workspace = builder.build(cfg, buggySource);

        MavenTestRunner runner = new MavenTestRunner("mvn", Duration.ofSeconds(60));

        // Act
        TestResult result = runner.runTests(workspace);

        // Assert
        assertFalse(result.isTimedOut(), "Should not time out");
        assertNotEquals(0, result.getExitCode(), "Buggy program should fail tests");
        assertFalse(result.isAllPassed(), "Buggy program should not pass all tests");
        assertNotNull(result.getOutput());
        assertFalse(result.getOutput().isBlank(), "Output should not be blank");
    }

    @Test
    void runTests_returnsSuccessForFixedProgram_whenTestsPass() throws IOException {
        // Arrange benchmark files
        Path buggyProgram = tempDir.resolve("buggy").resolve("Program.java");
        Path tests = tempDir.resolve("tests").resolve("ProgramTest.java");
        Files.createDirectories(buggyProgram.getParent());
        Files.createDirectories(tests.getParent());

        String fixedSource = """
                public class Program {
                    public static int add(int a, int b) { return a + b; }
                }
                """;
        String testSource = """
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;

                public class ProgramTest {
                    @Test void adds() {
                        assertEquals(3, Program.add(1,2));
                    }
                }
                """;
        Files.writeString(buggyProgram, fixedSource, StandardCharsets.UTF_8);
        Files.writeString(tests, testSource, StandardCharsets.UTF_8);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm-test");
        cfg.setBuggyProgramPath(buggyProgram);
        cfg.setTestSuitePath(tests);

        WorkspaceBuilder builder = new WorkspaceBuilder();
        Path workspace = builder.build(cfg, fixedSource);

        MavenTestRunner runner = new MavenTestRunner(Duration.ofSeconds(60));

        // Act
        TestResult result = runner.runTests(workspace);

        // Assert
        assertFalse(result.isTimedOut(), "Should not time out");
        assertEquals(0, result.getExitCode(), "Fixed program should pass tests");
        assertTrue(result.isAllPassed(), "Fixed program should pass all tests");
        assertNotNull(result.getOutput());
    }
}
