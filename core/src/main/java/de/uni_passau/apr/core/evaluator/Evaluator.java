package de.uni_passau.apr.core.evaluator;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;

public interface Evaluator {

    public EvaluationResult evaluate(BenchmarkConfig config, String candidateSource);
}
