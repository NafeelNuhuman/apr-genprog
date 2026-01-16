package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.patch.models.Patch;

/**
 * Class to hold evaluated candidate
 * - the patch
 * - its fitness (higher is better)
 * - the full evaluation result
 */
public record EvaluatedCandidate(Patch patch, double fitness, EvaluationResult evaluation) { }
