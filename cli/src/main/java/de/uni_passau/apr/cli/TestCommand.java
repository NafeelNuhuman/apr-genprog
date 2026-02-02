package de.uni_passau.apr.cli;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.benchmark.BenchmarkLoader;
import de.uni_passau.apr.core.testrunner.MavenTestRunner;
import de.uni_passau.apr.core.testrunner.TestResult;
import de.uni_passau.apr.core.workspace.WorkspaceBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "test",
        mixinStandardHelpOptions = true,
        description = "Used to run the tests of benchmarks."
)
public class TestCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = { "-br", "--benchmarks-root" },
            description = "Root directory where benchmarks are stored.",
            defaultValue = "benchmarks"
    )
    private String benchmarkRoot;

    @CommandLine.Option(
            names = { "-n", "--benchmark-name" },
            description = "Name of the benchmark to test."
    )
    private String benchmarkName;

    @Override
    public Integer call() {
        if (benchmarkName == null || benchmarkName.isEmpty()) {
            System.err.println("Benchmark name must be provided.");
            return 1;
        }

        System.out.printf("Testing benchmark '%s' in root directory '%s'%n", benchmarkName, benchmarkRoot);
        try {
            BenchmarkLoader loader = new BenchmarkLoader(java.nio.file.Path.of(benchmarkRoot));
            BenchmarkConfig config = loader.load(benchmarkName);
            WorkspaceBuilder workspaceBuilder = new WorkspaceBuilder();

            Path buggyWorkspace = workspaceBuilder.build(config, config.getBuggyProgram());
            System.out.printf("Test workspace for buggy program created at: %s%n", buggyWorkspace);
            Path fixedWorkspace = workspaceBuilder.build(config, config.getFixedProgram());
            System.out.printf("Test workspace for fixed program created at: %s%n", fixedWorkspace);

            MavenTestRunner testRunner = new MavenTestRunner(Duration.ofSeconds(20));

            System.out.println("Running tests on buggy program...");
            TestResult buggyResult = testRunner.runTests(buggyWorkspace);
            System.out.printf("Buggy program test results: Tests run: %d, Failures: %d, Errors: %d, Skipped: %d%n",
                    buggyResult.getTestsRun(), buggyResult.getFailures(), buggyResult.getErrors(), buggyResult.getSkipped());


            System.out.println("\n\nRunning tests on fixed program...");
            TestResult fixedResult = testRunner.runTests(fixedWorkspace);
            System.out.printf("Fixed program test results: Tests run: %d, Failures: %d, Errors: %d, Skipped: %d%n",
                    fixedResult.getTestsRun(), fixedResult.getFailures(), fixedResult.getErrors(), fixedResult.getSkipped());

        } catch (IOException e) {
            System.err.printf("Failed to load benchmark '%s': %s%n", benchmarkName, e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.printf("An error occurred while testing benchmark '%s': %s%n", benchmarkName, e.getMessage());
            return 1;
        }
        return 0;
    }
}
