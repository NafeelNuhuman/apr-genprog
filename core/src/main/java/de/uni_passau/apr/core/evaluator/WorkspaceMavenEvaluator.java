package de.uni_passau.apr.core.evaluator;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.testrunner.TestResult;
import de.uni_passau.apr.core.testrunner.TestRunner;
import de.uni_passau.apr.core.workspace.WorkspaceBuilder;
import de.uni_passau.apr.core.workspace.WorkspaceCleaner;

import java.nio.file.Path;

public class WorkspaceMavenEvaluator implements Evaluator {

    private WorkspaceBuilder workspaceBuilder;
    private TestRunner testRunner;
    private boolean keepWorkspaceAlways;
    private boolean keepWorkspaceOnFailure;

    public WorkspaceMavenEvaluator(WorkspaceBuilder workspaceBuilder, TestRunner testRunner, boolean keepWorkspaceAlways, boolean keepWorkspaceOnFailure) {
        if (workspaceBuilder == null) {
            throw new IllegalArgumentException("WorkspaceBuilder cannot be null");
        }
        if (testRunner == null) {
            throw new IllegalArgumentException("TestRunner cannot be null");
        }
        this.workspaceBuilder = workspaceBuilder;
        this.testRunner = testRunner;
        this.keepWorkspaceAlways = keepWorkspaceAlways;
        this.keepWorkspaceOnFailure = keepWorkspaceOnFailure;
    }

    @Override
    public EvaluationResult evaluate(BenchmarkConfig config, String candidateSource) {
        if (config == null) {
            throw new IllegalArgumentException("BenchmarkConfig cannot be null");
        }
        if (candidateSource == null || candidateSource.isEmpty()) {
            throw new IllegalArgumentException("Candidate source cannot be null or empty");
        }
        System.out.println("Evaluating candidate source in workspace...");

        EvaluationResult result = new EvaluationResult();
        Path workspaceDir;
        try {
            workspaceDir = workspaceBuilder.build(config, candidateSource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build workspace", e);
        }

        if (workspaceDir != null) {
            TestResult testResult = null;
            try {
                testResult = testRunner.runTests(workspaceDir);
                result.setTestResult(testResult);
            } catch (Exception e) {
                throw new RuntimeException("Failed to run tests in workspace", e);
            } finally {
                boolean testsFailed;
                if (testResult == null) {
                    testsFailed = true;
                } else {
                    testsFailed = !testResult.isAllPassed() || testResult.isTimedOut() || testResult.getExitCode() != 0;
                }
                if (keepWorkspaceAlways || (keepWorkspaceOnFailure && testsFailed)) {
                    result.setWorkSpaceKept(true);
                    result.setWorkspaceDir(workspaceDir);
                } else {
                    deleteWorkspace(workspaceDir);
                    result.setWorkSpaceKept(false);
                }
            }
        }
        return result;
    }

    private static void deleteWorkspace(Path workspaceDir) {
        try {
            // recursively delete the workspace directory
            WorkspaceCleaner.deleteRecursively(workspaceDir);
        } catch (Exception e) {
            System.err.println("Warning: Failed to delete workspace directory " + workspaceDir + ": " + e.getMessage());
        }
    }
}
