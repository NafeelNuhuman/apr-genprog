package de.uni_passau.apr.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_withoutBenchmarkName_returns1_andPrintsError() {
        TestCommand cmd = new TestCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute(); // no args

            assertEquals(1, exit);
            assertTrue(io.err().contains("Benchmark name must be provided."),
                    "Should print validation error to stderr");
        }
    }

    @Test
    void execute_withNonExistingBenchmarksRoot_returns1_andPrintsError() {
        Path missingRoot = tempDir.resolve("does-not-exist");

        TestCommand cmd = new TestCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute(
                    "-br", missingRoot.toString(),
                    "-n", "bm1"
            );

            assertEquals(1, exit);

            // BenchmarkLoader ctor throws IllegalArgumentException, caught by generic Exception branch
            assertTrue(io.err().contains("An error occurred while testing benchmark"),
                    "Should report generic testing error");
            assertTrue(io.err().contains("bm1"),
                    "Error should mention benchmark name");
        }
    }

    @Test
    void execute_withExistingRootButMissingBenchmark_returns1_andPrintsError() throws Exception {
        // create empty benchmark root dir
        Path root = Files.createDirectories(tempDir.resolve("benchmarks"));

        TestCommand cmd = new TestCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute(
                    "-br", root.toString(),
                    "-n", "bmMissing"
            );

            assertEquals(1, exit);

            // loader.load throws IllegalArgumentException (benchmark does not exist), caught by generic Exception
            assertTrue(io.err().contains("An error occurred while testing benchmark"),
                    "Should report generic testing error");
            assertTrue(io.err().contains("bmMissing"));
        }
    }

    @Test
    void parsing_defaults_work_benchmarksRootDefaultsToBenchmarks_andFileDefaultsToAll() {
        TestCommand cmd = new TestCommand();
        CommandLine cli = new CommandLine(cmd);

        // We cannot safely run full command (it will try to load & run maven),
        // but we can verify parsing sets defaults by using CommandLine.parseArgs
        cli.parseArgs("-n", "bm1");

        // Access private fields via reflection just for verification.
        assertEquals("benchmarks", (String) getField(cmd, "benchmarkRoot"));
        assertEquals("bm1", (String) getField(cmd, "benchmarkName"));
        assertEquals("all", (String) getField(cmd, "buggyOrFixedFile"));
    }

    @Test
    void parsing_fileOption_setsValue() {
        TestCommand cmd = new TestCommand();
        CommandLine cli = new CommandLine(cmd);

        cli.parseArgs("-n", "bm1", "-f", "b");

        assertEquals("b", (String) getField(cmd, "buggyOrFixedFile"));
    }

    // ---------------- helpers ----------------

    private static Object getField(Object target, String fieldName) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Cannot read field: " + fieldName, e);
        }
    }

    /**
     * Captures System.out and System.err for assertions.
     */
    private static final class CapturedIO implements AutoCloseable {
        private final PrintStream oldOut = System.out;
        private final PrintStream oldErr = System.err;

        private final ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();

        CapturedIO() {
            System.setOut(new PrintStream(outBuf, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBuf, true, StandardCharsets.UTF_8));
        }

        String out() {
            return outBuf.toString(StandardCharsets.UTF_8);
        }

        String err() {
            return errBuf.toString(StandardCharsets.UTF_8);
        }

        @Override
        public void close() {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
}
