package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.evaluator.EvaluationResult;

public record RepairResult(String bestCandidateSource, EvaluationResult evaluationResult, boolean repairedSuccessfully) {
    public RepairResult {
        if (bestCandidateSource == null) {
            throw new IllegalArgumentException("bestCandidateSource cannot be null");
        }
        if (evaluationResult == null) {
            throw new IllegalArgumentException("evaluationResult cannot be null");
        }
    }
}
