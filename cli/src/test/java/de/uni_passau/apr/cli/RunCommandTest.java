package de.uni_passau.apr.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RunCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_withoutNameAndWithoutAll_returns2_andPrintsError() {
        RunCommand cmd = new RunCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute(); // no args
            assertEquals(2, exit);
            assertTrue(io.err().contains("Either --benchmark-name must be specified or --all must be set to true"));
        }
    }

    @Test
    void execute_withAllAndName_returns2_andPrintsError() {
        RunCommand cmd = new RunCommand();
        CommandLine cli = new CommandLine(cmd);

        CapturedIO io = new CapturedIO();
        try (io) {
            int exit = cli.execute("--all", "-n", "bm1");
            assertEquals(2, exit);
            assertTrue(io.err().contains("Cannot specify --benchmark-name when --all is set to true"));
        }
    }

    @Test
    void parsing_defaults_areSet() throws Exception {
        RunCommand cmd = new RunCommand();
        CommandLine cli = new CommandLine(cmd);

        cli.parseArgs("--all"); // just to satisfy validation if we were to execute

        assertEquals("benchmarks", (String) getField(cmd, "benchmarkRoot"));
        assertEquals(false, (boolean) getField(cmd, "keepWorkspace"));
        assertEquals(42L, (long) getField(cmd, "seed"));
        assertEquals(20, (int) getField(cmd, "timeoutSeconds"));
        assertEquals(10, (int) getField(cmd, "populationSize"));
    }

    @Test
    void parsing_customValues_areApplied() {
        RunCommand cmd = new RunCommand();
        CommandLine cli = new CommandLine(cmd);

        cli.parseArgs(
                "-br", tempDir.toString(),
                "--all",
                "-kp",
                "-s", "123",
                "-t", "99",
                "-p", "77"
        );

        assertEquals(tempDir.toString(), (String) getField(cmd, "benchmarkRoot"));
        assertEquals(true, (boolean) getField(cmd, "keepWorkspace"));
        assertEquals(123L, (long) getField(cmd, "seed"));
        assertEquals(99, (int) getField(cmd, "timeoutSeconds"));
        assertEquals(77, (int) getField(cmd, "populationSize"));
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
