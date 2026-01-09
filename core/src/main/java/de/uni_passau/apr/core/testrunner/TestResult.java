package de.uni_passau.apr.core.testrunner;

public class TestResult {
    private int exitCode;
    private String output;
    private boolean allPassed;
    private boolean timedOut;

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
}
