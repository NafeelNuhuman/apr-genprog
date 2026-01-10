package de.uni_passau.apr.core.service;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.benchmark.BenchmarkLoader;
import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import de.uni_passau.apr.core.faultlocalization.FaultLocalizationProvider;

import java.io.IOException;


/**
 * High-level service for loading and repairing programs.
 */
public class RepairService {

    private final BenchmarkLoader benchmarkLoader;
    private final Evaluator evaluator;
    private final FaultLocalizationProvider faultLocProvider;

    public RepairService(BenchmarkLoader benchmarkLoader, Evaluator evaluator, FaultLocalizationProvider faultLocProvider) {
        if (benchmarkLoader == null) {
            throw new IllegalArgumentException("BenchmarkLoader cannot be null");
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("Evaluator cannot be null");
        }
        if (faultLocProvider == null) {
            throw new IllegalArgumentException("FaultLocProvider cannot be null");
        }
        this.benchmarkLoader = benchmarkLoader;
        this.evaluator = evaluator;
        this.faultLocProvider = faultLocProvider;
    }

    public BenchmarkConfig loadBenchmark(String name) throws IOException {
        BenchmarkConfig config;
        config = benchmarkLoader.load(name);
        return config;
    }

    public FaultLocalization loadFaultLocalization(BenchmarkConfig config) throws IOException {
        FaultLocalization faultLocalization;
        faultLocalization = faultLocProvider.loadFor(config);
        return faultLocalization;
    }

    public LoadedBenchmark loadBenchmarkWithFaultLoc(String name) throws IOException {
        BenchmarkConfig config = loadBenchmark(name);
        FaultLocalization faultLocalization = loadFaultLocalization(config);
        return new LoadedBenchmark(config, faultLocalization);
    }

    public EvaluationResult evaluateCandidate(BenchmarkConfig config, String candidateSource) {
        return evaluator.evaluate(config, candidateSource);
    }

}


