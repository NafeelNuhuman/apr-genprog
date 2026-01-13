package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.patch.models.Patch;

/**
 * Holds everything GenProg needs after evaluating a patch:
 * - the patch itself
 * - its numeric fitness (higher is better)
 * - the full evaluation result (test output, workspace path, etc.)
 */
public record EvaluatedCandidate(Patch patch, double fitness, EvaluationResult evaluation) { }
