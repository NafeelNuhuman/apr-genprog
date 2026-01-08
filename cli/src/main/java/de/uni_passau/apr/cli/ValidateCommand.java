package de.uni_passau.apr.cli;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.benchmark.BenchmarkLoader;
import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import de.uni_passau.apr.core.faultlocalization.json.JsonFaultLocProvider;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        description = "Validates a benchmark directory structure."
)
public class ValidateCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-br", "--benchmarks-root"},
            description = "Root directory where benchmarks are stored.",
            defaultValue = "benchmarks"
    )
    private String benchmarkRoot;

    @CommandLine.Option(
            names = {"-n", "--benchmark-name"},
            description = "Name of the benchmark to validate."
    )
    private String benchmarkName;

    @CommandLine.Option(
            names = {"-a", "--all"},
            description = "Validate all benchmarks in the root directory.",
            defaultValue = "false"
    )
    private boolean validateAll;

    @Override
    public Integer call() {
        if (benchmarkName == null && !validateAll) {
            System.err.println("Error: Either --benchmark-name or --all must be specified.");
            return 1;
        }
        System.out.println("Validating benchmark: " + benchmarkName + " in root: " + benchmarkRoot);
        BenchmarkLoader loader = new BenchmarkLoader(Path.of(benchmarkRoot));
        if (!validateAll) {
            return validateBenchmarkAndFaultLoc(loader, benchmarkName);
        } else {
            int overallStatus = 0;
            try {
                for (String benchmark : loader.listAvailableBenchmarks()) {
                    System.out.println("-- Validating benchmark: " + benchmark);
                    int status = validateBenchmarkAndFaultLoc(loader, benchmark);
                    if (status != 0) {
                        overallStatus = status;
                    }
                }
            } catch (IOException e) {
                System.err.println("IO error while listing benchmarks in root: " + benchmarkRoot);
                System.err.println("Reason: " + e.getMessage());
                return 2;
            }
            return overallStatus;
        }
    }

    private int validateBenchmarkAndFaultLoc(BenchmarkLoader loader, String benchmarkName) {
        BenchmarkConfig config;
        try {
            config = loader.load(benchmarkName);
            System.out.println("Benchmark " + benchmarkName + " is valid");
        } catch (IllegalArgumentException e) {
            System.err.println("Benchmark " + benchmarkName + " is invalid");
            System.err.println("Reason: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println("IO error while validating benchmark " + benchmarkName);
            System.err.println("Reason: " + e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Unexpected error while validating benchmark " + benchmarkName);
            System.err.println("Reason: " + e.getMessage());
            return 1;
        }

        // Validate faultloc.json
        JsonFaultLocProvider faultLocProvider = new JsonFaultLocProvider();
        try {
            FaultLocalization faultLocalization = faultLocProvider.loadFor(config);
            System.out.println("Fault localization file is valid. Non-zero weighter statements: " +
                    faultLocalization.getStatements().size());
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Fault localization file is invalid");
            System.err.println("Reason: " + e.getMessage());
            return 3;
        } catch (IOException e) {
            System.err.println("IO error while validating fault localization file");
            System.err.println("Reason: " + e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Unexpected error while validating fault localization file");
            System.err.println("Reason: " + e.getMessage());
            return 10;
        }
    }
}