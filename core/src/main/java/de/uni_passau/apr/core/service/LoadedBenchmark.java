package de.uni_passau.apr.core.service;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.faultlocalization.FaultLocalization;

public record LoadedBenchmark(BenchmarkConfig config, FaultLocalization faultLocalization) {
    public LoadedBenchmark {
        if (config == null) throw new IllegalArgumentException("config cant be null");
        if (faultLocalization == null) throw new IllegalArgumentException("fault localization cant be null");
    }
}
