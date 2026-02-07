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

class ValidateCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_withoutNameAndWithoutAll_returns1_andPrintsError() {
        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute(); // no args
            assertEquals(1, exit);
            assertTrue(io.err().contains("Either --benchmark-name or --all must be specified"));
        }
    }

    @Test
    void execute_singleBenchmark_validStructureAndFaultLoc_returns0() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("benchmarks"));
        createValidBenchmark(root, "bm1");

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute("-br", root.toString(), "-n", "bm1");

            assertEquals(0, exit);
            assertTrue(io.out().contains("Benchmark bm1 is valid"));
            assertTrue(io.out().contains("Fault localization file is valid"));
        }
    }

    @Test
    void execute_singleBenchmark_invalidStructure_returns1() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("benchmarks"));
        // create benchmark dir but missing required subdirs/files
        Files.createDirectories(root.resolve("bmBad"));

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute("-br", root.toString(), "-n", "bmBad");

            assertEquals(1, exit);
            assertTrue(io.err().contains("Benchmark bmBad is invalid"));
        }
    }

    @Test
    void execute_singleBenchmark_faultLocInvalid_returns3() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("benchmarks"));
        createValidBenchmark(root, "bm2");

        // overwrite faultloc.json with invalid weights (e.g., 0.2 not allowed)
        Path faultLoc = root.resolve("bm2").resolve("faultloc.json");
        Files.writeString(faultLoc,
                """
                {
                  "file": "Buggy.java",
                  "statements": [ {"line": 1, "weight": 0.2} ]
                }
                """,
                StandardCharsets.UTF_8
        );

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute("-br", root.toString(), "-n", "bm2");

            assertEquals(3, exit);
            assertTrue(io.err().contains("Fault localization file is invalid"));
        }
    }

    @Test
    void execute_allBenchmarks_returnsNonZeroIfAnyBenchmarkInvalid() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("benchmarks"));

        createValidBenchmark(root, "bmOk");
        Files.createDirectories(root.resolve("bmBad")); // invalid structure

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute("-br", root.toString(), "--all");

            // At least one invalid benchmark => overallStatus becomes non-zero.
            assertNotEquals(0, exit);
            assertTrue(io.out().contains("-- Validating benchmark: bmOk"));
            assertTrue(io.out().contains("-- Validating benchmark: bmBad"));
        }
    }

    @Test
    void execute_allBenchmarks_allValid_returns0() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("benchmarks"));
        createValidBenchmark(root, "bm1");
        createValidBenchmark(root, "bm2");

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute("-br", root.toString(), "--all");

            assertEquals(0, exit);
            assertTrue(io.out().contains("Fault localization file is valid"));
        }
    }

    // ---------------- helpers ----------------

    /**
     * Creates a benchmark directory layout that matches BenchmarkLoader expectations:
     * <root>/<bm>/
     *   buggy/  (exactly one .java, non-blank)
     *   fixed/  (exactly one .java, non-blank)
     *   tests/  (exactly one .java, non-blank)
     *   faultloc.json (exists)
     *
     * Also ensures buggy program has enough lines for faultloc line numbers.
     */
    private void createValidBenchmark(Path root, String name) throws Exception {
        Path bm = Files.createDirectories(root.resolve(name));

        Path buggyDir = Files.createDirectories(bm.resolve("buggy"));
        Path fixedDir = Files.createDirectories(bm.resolve("fixed"));
        Path testsDir = Files.createDirectories(bm.resolve("tests"));

        // 3-line buggy program (so faultloc line 1..3 is in range)
        Files.writeString(buggyDir.resolve("Buggy.java"),
                "public class Buggy {\n" +
                        "  public int f(){ return 1; }\n" +
                        "}\n",
                StandardCharsets.UTF_8);

        Files.writeString(fixedDir.resolve("Fixed.java"),
                "public class Fixed { public int f(){ return 2; } }\n",
                StandardCharsets.UTF_8);

        Files.writeString(testsDir.resolve("ProgramTest.java"),
                "public class ProgramTest { }\n",
                StandardCharsets.UTF_8);

        // valid faultloc: line 2 weight 0.1 (allowed), no duplicates, within range
        Files.writeString(bm.resolve("faultloc.json"),
                """
                {
                  "file": "Buggy.java",
                  "statements": [ {"line": 2, "weight": 0.1} ]
                }
                """,
                StandardCharsets.UTF_8);
    }

    private static final class CapturedIO implements AutoCloseable {
        private final PrintStream oldOut = System.out;
        private final PrintStream oldErr = System.err;

        private final ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();

        CapturedIO() {
            System.setOut(new PrintStream(outBuf, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBuf, true, StandardCharsets.UTF_8));
        }

        String out() { return outBuf.toString(StandardCharsets.UTF_8); }
        String err() { return errBuf.toString(StandardCharsets.UTF_8); }

        @Override
        public void close() {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
}
