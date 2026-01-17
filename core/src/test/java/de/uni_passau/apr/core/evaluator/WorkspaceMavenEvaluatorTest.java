package de.uni_passau.apr.core.evaluator;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.testrunner.TestResult;
import de.uni_passau.apr.core.testrunner.TestRunner;
import de.uni_passau.apr.core.workspace.WorkspaceBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceMavenEvaluatorTest {

    @Test
    void evaluate_deletesWorkspace_whenKeepAlwaysFalse_andKeepOnFailureFalse_andTestsPass() throws Exception {
        // Arrange: workspace under system temp with apr- prefix
        Path workspace = createAprTempWorkspace();

        WorkspaceBuilder builder = new FakeBuilder(workspace);

        TestRunner runner = dir -> {
            TestResult tr = new TestResult();
            tr.setExitCode(0);
            tr.setAllPassed(true);
            tr.setTimedOut(false);
            return tr;
        };

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, false);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");

        // Act
        EvaluationResult result = evaluator.evaluate(cfg, "public class Program {}");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTestResult());
        assertTrue(result.getTestResult().isAllPassed());
        assertFalse(result.isWorkSpaceKept(), "Workspace should not be kept on success with keep flags disabled");
        assertFalse(Files.exists(workspace), "Workspace directory should be deleted");
    }

    @Test
    void evaluate_keepsWorkspace_whenKeepAlwaysTrue_evenIfTestsPass() throws Exception {
        Path workspace = createAprTempWorkspace();

        WorkspaceBuilder builder = new FakeBuilder(workspace);

        TestRunner runner = dir -> {
            TestResult tr = new TestResult();
            tr.setExitCode(0);
            tr.setAllPassed(true);
            tr.setTimedOut(false);
            return tr;
        };

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, true, false);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");

        EvaluationResult result = evaluator.evaluate(cfg, "public class Program {}");

        assertTrue(result.isWorkSpaceKept(), "Workspace should be kept when keepWorkspaceAlways=true");
        assertTrue(Files.exists(workspace), "Workspace directory should still exist");
    }

    @Test
    void evaluate_keepsWorkspace_whenKeepOnFailureTrue_andTestsFail() throws Exception {
        Path workspace = createAprTempWorkspace();

        WorkspaceBuilder builder = new FakeBuilder(workspace);

        TestRunner runner = dir -> {
            TestResult tr = new TestResult();
            tr.setExitCode(1);
            tr.setAllPassed(false);
            tr.setTimedOut(false);
            return tr;
        };

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, true);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");

        EvaluationResult result = evaluator.evaluate(cfg, "public class Program {}");

        assertFalse(result.getTestResult().isAllPassed());
        assertTrue(result.isWorkSpaceKept(), "Workspace should be kept when keepWorkspaceOnFailure=true and tests fail");
        assertTrue(Files.exists(workspace), "Workspace directory should still exist");
    }

    @Test
    void evaluate_deletesWorkspace_whenKeepOnFailureTrue_butTestsPass() throws Exception {
        Path workspace = createAprTempWorkspace();

        WorkspaceBuilder builder = new FakeBuilder(workspace);

        TestRunner runner = dir -> {
            TestResult tr = new TestResult();
            tr.setExitCode(0);
            tr.setAllPassed(true);
            tr.setTimedOut(false);
            return tr;
        };

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, true);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");

        EvaluationResult result = evaluator.evaluate(cfg, "public class Program {}");

        assertTrue(result.getTestResult().isAllPassed());
        assertFalse(result.isWorkSpaceKept(), "Workspace should not be kept on pass even if keepOnFailure=true");
        assertFalse(Files.exists(workspace), "Workspace directory should be deleted");
    }

    @Test
    void evaluate_keepsWorkspace_whenKeepOnFailureTrue_andRunnerTimesOut() throws Exception {
        Path workspace = createAprTempWorkspace();

        WorkspaceBuilder builder = new FakeBuilder(workspace);

        TestRunner runner = dir -> {
            TestResult tr = new TestResult();
            tr.setExitCode(124);
            tr.setAllPassed(false);
            tr.setTimedOut(true);
            return tr;
        };

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, true);

        BenchmarkConfig cfg = new BenchmarkConfig();
        cfg.setName("bm1");

        EvaluationResult result = evaluator.evaluate(cfg, "public class Program {}");

        assertTrue(result.getTestResult().isTimedOut());
        assertTrue(result.isWorkSpaceKept(), "Workspace should be kept on timeout when keepOnFailure=true");
        assertTrue(Files.exists(workspace), "Workspace directory should still exist");
    }

    @Test
    void evaluate_throwsIllegalArgumentException_onNullConfigOrEmptyCandidate() throws Exception {
        WorkspaceBuilder builder = new FakeBuilder(createAprTempWorkspace());
        TestRunner runner = dir -> new TestResult();

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, false);

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(new BenchmarkConfig(), ""));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(new BenchmarkConfig(), null));
    }

    @Test
    void evaluate_wrapsWorkspaceBuildFailure() {
        WorkspaceBuilder builder = new WorkspaceBuilder() {
            @Override
            public Path build(BenchmarkConfig benchmarkConfig, String candidate) throws IOException {
                throw new IOException("boom");
            }
        };

        TestRunner runner = dir -> new TestResult();

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> evaluator.evaluate(new BenchmarkConfig(), "public class Program {}"));

        assertTrue(ex.getMessage().toLowerCase().contains("build workspace"));
    }

    @Test
    void evaluate_whenRunnerThrows_doesNotDeleteWorkspace_ifKeepOnFailureTrue() throws Exception {
        // This test expects you to handle testResult==null safely in finally.
        // If your evaluator currently NPEs in finally, this test will fail (which is good: it reveals the bug).
        Path workspace = createAprTempWorkspace();

        WorkspaceBuilder builder = new FakeBuilder(workspace);

        TestRunner runner = dir -> {
            throw new RuntimeException("runner crashed");
        };

        WorkspaceMavenEvaluator evaluator =
                new WorkspaceMavenEvaluator(builder, runner, false, true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> evaluator.evaluate(new BenchmarkConfig(), "public class Program {}"));

        assertTrue(ex.getMessage().toLowerCase().contains("run tests"));
        assertTrue(Files.exists(workspace),
                "Workspace should remain for debugging if runner crashed and keepOnFailure=true");
    }

    // ---- helpers ----

    private static Path createAprTempWorkspace() throws IOException {
        // must be under java.io.tmpdir and start with apr- to satisfy WorkspaceCleaner guards
        Path ws = Files.createTempDirectory("apr-eval-test-");
        Files.createDirectories(ws.resolve("src/main/java"));
        Files.writeString(ws.resolve("pom.xml"), "<project/>");
        return ws;
    }

    private static final class FakeBuilder extends WorkspaceBuilder {
        private final Path workspace;

        private FakeBuilder(Path workspace) {
            this.workspace = workspace;
        }

        @Override
        public Path build(BenchmarkConfig benchmarkConfig, String candidate) {
            return workspace;
        }
    }
}
