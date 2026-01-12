package de.uni_passau.apr.core.utils;

import java.util.List;

public class TestReportSummary {

    private int testsRun;
    private int failures;
    private int errors;
    private int skipped;
    private List<String> failedTestsIDs;

    public TestReportSummary() {
    }

    public TestReportSummary(int testsRun, int failures, int errors, int skipped, List<String> failedTestsIDs) {
        this.testsRun = testsRun;
        this.failures = failures;
        this.errors = errors;
        this.skipped = skipped;
        this.failedTestsIDs = failedTestsIDs;
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

    public List<String> getFailedTestsIDs() {
        return failedTestsIDs;
    }

    public void setFailedTestsIDs(List<String> failedTestsIDs) {
        this.failedTestsIDs = failedTestsIDs;
    }
}
