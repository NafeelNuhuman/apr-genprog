package de.uni_passau.apr.core.evaluator;

import de.uni_passau.apr.core.testrunner.TestResult;

import java.nio.file.Path;
import java.util.Objects;

public class EvaluationResult {

    private TestResult testResult;
    private Path workspaceDir;
    private boolean workSpaceKept;

    public EvaluationResult() {
    }

    public EvaluationResult(TestResult testResult, Path workspaceDir, boolean workSpaceKept) {
        this.testResult = testResult;
        this.workspaceDir = workspaceDir;
        this.workSpaceKept = workSpaceKept;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }

    public Path getWorkspaceDir() {
        return workspaceDir;
    }

    public void setWorkspaceDir(Path workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public boolean isWorkSpaceKept() {
        return workSpaceKept;
    }

    public void setWorkSpaceKept(boolean workSpaceKept) {
        this.workSpaceKept = workSpaceKept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationResult that = (EvaluationResult) o;
        return workSpaceKept == that.workSpaceKept && Objects.equals(testResult, that.testResult) && Objects.equals(workspaceDir, that.workspaceDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testResult, workspaceDir, workSpaceKept);
    }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "testResult=" + testResult +
                ", workspaceDir=" + workspaceDir +
                ", workSpaceKept=" + workSpaceKept +
                '}';
    }
}
