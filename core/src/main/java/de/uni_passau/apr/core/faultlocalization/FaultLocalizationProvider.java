package de.uni_passau.apr.core.faultlocalization;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;

public interface FaultLocalizationProvider {

    FaultLocalization loadFor(BenchmarkConfig benchmarkConfig);
}
