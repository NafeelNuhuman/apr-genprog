package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.service.LoadedBenchmark;

public interface RepairAlgorithm {

    RepairResult run(LoadedBenchmark benchmark, RunConfig runConfig);

}