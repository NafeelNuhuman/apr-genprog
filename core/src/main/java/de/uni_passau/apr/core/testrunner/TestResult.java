package de.uni_passau.apr.core.testrunner;

import java.util.ArrayList;
import java.util.List;

public class TestResult {
    private int exitCode;
    private String output;
    private boolean allPassed;
    private boolean timedOut;
    private int testsRun = 0;
    private int failures = 0;
    private int errors = 0;
    private int skipped = 0;
    private List<String> failedTests = new ArrayList<>();

    public TestResult() {
    }

    public TestResult(int exitCode, String output, boolean allPassed, boolean timedOut) {
        this.exitCode = exitCode;
        this.output = output;
        this.allPassed = allPassed;
        this.timedOut = timedOut;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean isAllPassed() {
        return allPassed;
    }

    public void setAllPassed(boolean allPassed) {
        this.allPassed = allPassed;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public int getTestsRun() {
        return testsRun;
    }

    public void setTestsRun(int testsRun) {
        this.testsRun = testsRun;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public List<String> getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(List<String> failedTests) {
        this.failedTests = failedTests;
    }
}
