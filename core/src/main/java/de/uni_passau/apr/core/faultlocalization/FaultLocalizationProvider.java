package de.uni_passau.apr.core.faultlocalization;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;

import java.io.IOException;

public interface FaultLocalizationProvider {

    FaultLocalization loadFor(BenchmarkConfig benchmarkConfig) throws IOException;
}
