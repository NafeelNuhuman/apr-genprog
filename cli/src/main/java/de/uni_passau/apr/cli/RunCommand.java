package de.uni_passau.apr.cli;

import de.uni_passau.apr.core.algorithm.GenProgEngine;
import de.uni_passau.apr.core.algorithm.RepairResult;
import de.uni_passau.apr.core.algorithm.RunConfig;
import de.uni_passau.apr.core.benchmark.BenchmarkLoader;
import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.evaluator.WorkspaceMavenEvaluator;
import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;
import de.uni_passau.apr.core.faultlocalization.json.JsonFaultLocProvider;
import de.uni_passau.apr.core.fitness.FitnessEvaluator;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.selection.PopulationInitializer;
import de.uni_passau.apr.core.service.LoadedBenchmark;
import de.uni_passau.apr.core.service.RepairService;
import de.uni_passau.apr.core.testrunner.MavenTestRunner;
import de.uni_passau.apr.core.testrunner.TestRunner;
import de.uni_passau.apr.core.workspace.WorkspaceBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "run",
        mixinStandardHelpOptions = true,
        description = "Runs the APR tool on a specified benchmark."
)
public class RunCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-br", "--benchmarks-root"},
            description = "Root directory where benchmarks are stored.",
            defaultValue = "benchmarks"
    )
    private String benchmarkRoot;

    @CommandLine.Option(
            names = {"-n", "--benchmark-name"},
            description = "Name of the benchmark to run."
    )
    private String benchmarkName;

    @CommandLine.Option(
            names = {"-a", "--all"},
            description = "Run all benchmarks in the root directory.",
            defaultValue = "false"
    )
    private boolean runAll;

    @CommandLine.Option(
            names = {"-t", "--timeout-seconds"},
            description = "Timeout in seconds for each benchmark run.",
            defaultValue = "20"
    )
    private int timeoutSeconds;

    @CommandLine.Option(
            names = {"-kp", "--keep-workspace"},
            description = "Keep the workspace after execution for debugging purposes.",
            defaultValue = "false"
    )
    private boolean keepWorkspace;

    @CommandLine.Option(
            names = {"-s", "--seed"},
            description = "Random seed for reproducibility.",
            defaultValue = "42"
    )
    private long seed;

    @CommandLine.Option(
            names = { "-p", "--population-size" },
            description = "Population size for the GenProg algorithm.",
            defaultValue = "10"
    )
    private int populationSize;

    @Override
    public Integer call() {
        if (!runAll && (benchmarkName == null || benchmarkName.isEmpty())) {
            System.err.println("Error: Either --benchmark-name must be specified or --all must be set to true.");
            return 2;
        }
        if (runAll && (benchmarkName != null && !benchmarkName.isEmpty())) {
            System.err.println("Error: Cannot specify --benchmark-name when --all is set to true.");
            return 2;
        }
        System.out.println("Running APR tool in root: " + benchmarkRoot);
        BenchmarkLoader loader = new BenchmarkLoader(java.nio.file.Path.of(benchmarkRoot));
        WorkspaceBuilder workspaceBuilder = new WorkspaceBuilder();
        TestRunner testRunner = new MavenTestRunner(Duration.ofSeconds(20));
        Evaluator evaluator = new WorkspaceMavenEvaluator(workspaceBuilder, testRunner, keepWorkspace, true);
        RepairService repairService = new RepairService(loader, evaluator, new JsonFaultLocProvider());
        Random random = new Random(seed);
        int resultCode = 0;
        if (!runAll) {
            System.out.println("Running single benchmark: " + benchmarkName);
            try {
                resultCode = repairWithGenProg(benchmarkName, repairService, evaluator, random);
            } catch (IOException e) {
                System.err.println("Error running benchmark: " + e.getMessage());
                return 1;
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                return 2;
            }

        } else {
            System.out.println("Running all benchmarks in root: " + benchmarkRoot);
            try {
                for (String benchmarkName : loader.listAvailableBenchmarks()) {
                    resultCode = repairWithGenProg(benchmarkName, repairService, evaluator, random);
                }
            } catch (IOException e) {
                System.err.println("Error running benchmarks: " + e.getMessage());
                return 1;
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                return 2;
            }
        }
        return resultCode;
    }

    private int repairWithGenProg(String benchmarkName, RepairService repairService, Evaluator evaluator, Random random) throws Exception {
        LoadedBenchmark benchmark = repairService.loadBenchmarkWithFaultLoc(benchmarkName);
        StatementCollector statementCollector = StatementCollector.fromFile(
                benchmark.config().getBuggyProgramPath()
        );
        FaultLocPrioratizedSampler sampler = new FaultLocPrioratizedSampler(
                benchmark.faultLocalization(),
                statementCollector,
                random
        );
        PopulationInitializer initializer = new PopulationInitializer(
                populationSize,
                random,
                statementCollector,
                sampler,
                0.10,
                true
        );
        RepairResult result = new GenProgEngine(
                initializer,
                new FitnessEvaluator(),
                evaluator,
                new SingleEditCrossover(random, statementCollector, true),
                new SingleEditMutator(0.06, random, statementCollector, sampler, false)
        ).run(benchmark, new RunConfig(50, populationSize, timeoutSeconds, random));

        if (result.repairedSuccessfully()) {
            System.out.println("Repair successful for benchmark: " + benchmarkName);
            System.out.println("Repaired Source Code:\n" + result.bestCandidateSource());
            return 0;
        } else {
            System.out.println("Repair failed for benchmark: " + benchmarkName);
            return 1;
        }
    }
}
