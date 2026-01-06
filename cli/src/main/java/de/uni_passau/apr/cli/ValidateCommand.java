package de.uni_passau.apr.cli;

import de.uni_passau.apr.core.benchmark.BenchmarkLoader;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "validate",
        mixinStandardHelpOptions = true,
        description = "Validates a benchmark directory structure."
)
public class ValidateCommand implements Callable {

    @CommandLine.Option(
            names = {"-br", "--benchmarks-root"},
            description = "Root directory where benchmarks are stored.",
            defaultValue = "benchmarks"
    )
    private String benchmarkRoot;

    @CommandLine.Option(
            names = {"-n", "--benchmark-name"},
            description = "Name of the benchmark to validate.",
            required = true
    )
    private String benchmarkName;

    @Override
    public Integer call() {
        System.out.println("Validating benchmark: " + benchmarkName + " in root: " + benchmarkRoot);
        BenchmarkLoader loader = new BenchmarkLoader(Path.of(benchmarkRoot));
        try {
            loader.load(benchmarkName);
            System.out.println("Benchmark " + benchmarkName + " is valid");
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Benchmark " + benchmarkName + " is invalid");
            System.err.println("Reason: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println("IO error while validating benchmark " + benchmarkName );
            System.err.println("Reason: " + e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Unexpected error while validating benchmark " + benchmarkName );
            System.err.println("Reason: " + e.getMessage());
            return 10;
        }
    }
}
