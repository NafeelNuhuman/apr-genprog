package de.uni_passau.apr.core.testrunner;

import java.nio.file.Files;
import java.time.Duration;

/**
 * A TestRunner implementation that uses Maven to run tests.
 */
public class MavenTestRunner implements TestRunner {

    private String mvnCommand = "mvn";
    private Duration timeout = Duration.ofSeconds(50);

    public MavenTestRunner() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            mvnCommand += ".cmd";
        }
    }

    public MavenTestRunner(Duration timeout) {
        this();
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be a positive duration");
        }
        this.timeout = timeout;
    }

    public MavenTestRunner(String mvnCommand, Duration timeout) {
        if (mvnCommand == null || mvnCommand.isBlank()) {
            throw new IllegalArgumentException("Maven command must not be null or blank");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be a positive duration");
        }
        // OS aware command adjustment
        if (System.getProperty("os.name").toLowerCase().contains("win") && !mvnCommand.endsWith(".cmd")) {
            mvnCommand += ".cmd";
        }
        this.mvnCommand = mvnCommand;
        this.timeout = timeout;
    }

    /**
     * Runs Maven tests in the specified workspace directory.
     *
     * @param workspaceDir the directory containing the Maven project
     * @return TestResult containing the results of the test execution
     * @throws IllegalArgumentException if workspaceDir is null or does not exist
     */
    @Override
    public TestResult runTests(java.nio.file.Path workspaceDir) {
        if (workspaceDir == null || !Files.isDirectory(workspaceDir)) {
            throw new IllegalArgumentException("Workspace directory is null or does not exist: " + workspaceDir);
        }
        // set working directory to workspaceDir
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workspaceDir.toFile());
        processBuilder.command(mvnCommand, "-q", "test");
        processBuilder.redirectErrorStream(true);
        TestResult result = new TestResult();
        try {
            Process process = processBuilder.start();
            // reader the output stream in a separate thread
            StringBuilder output = new StringBuilder();
            Thread outputReader = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                } catch (java.io.IOException ioe) {
                    output.append("IOException while reading process output: ").append(ioe.getMessage());
                }
            });
            outputReader.start();
            // wait for process to finish with timeout
            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                outputReader.join();
                result.setTimedOut(true);
                result.setExitCode(124);
                result.setOutput(output.toString() + "\n\nTest execution timed out after " + timeout.toSeconds() + " seconds.");
                result.setAllPassed(false);
                return result;
            }
            outputReader.join();
            int exitCode = process.exitValue();
            result.setExitCode(exitCode);
            result.setOutput(output.toString());
            result.setTimedOut(false);
            // determine if all tests passed
            result.setAllPassed(exitCode == 0);
        } catch (java.io.IOException ioe) {
            result.setExitCode(127);
            result.setOutput("IOException during test execution: " + ioe.getMessage());
            result.setAllPassed(false);
        } catch (Exception e) {
            result.setExitCode(10);
            result.setOutput("Exception during test execution: " + e.getMessage());
            result.setAllPassed(false);
        }
        return result;
    }
}
