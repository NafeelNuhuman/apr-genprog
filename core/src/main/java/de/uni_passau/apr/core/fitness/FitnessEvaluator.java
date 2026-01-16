package de.uni_passau.apr.core.fitness;

import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.testrunner.TestResult;

/**
 *
 * 1. Converting a Patch into candidate source code (use PatchApplier)
 * 2. Computing a fitness score from TestResult
 *
 */
public final class FitnessEvaluator {

    // Fitness weights ( WPosT=1, WNegT=10)
    private final double wPosT;
    private final double wNegT;

    public FitnessEvaluator(double wPosT, double wNegT) {
        if (wNegT <= 0) throw new IllegalArgumentException("wNegT must be > 0");
        this.wPosT = wPosT;
        this.wNegT = wNegT;
    }


    /**
     * Fitness function:
     *  1.Reward passed tests lightly (wPosT)
     *  2.Penalize failing tests heavily (wNegT)
     *  3.Penalize timeout
     *  4.Give a large bonus if all tests pass (To set best ones apart)
     * Higher fitness = better.
     */
    public double computeFitness(EvaluationResult eval) {
        if (eval == null || eval.getTestResult() == null) return Double.NEGATIVE_INFINITY;

        TestResult tr = eval.getTestResult();

        int bad = safeNonNeg(tr.getFailures()) + safeNonNeg(tr.getErrors());
        int run = safeNonNeg(tr.getTestsRun());
        int skipped = safeNonNeg(tr.getSkipped());
        int passed = Math.max(0, run - skipped - bad);

        double score = (wPosT * passed) - (wNegT * bad);

        // Extra penalties for unstable ones
        if (tr.isTimedOut()) score -= 1000.0;
        if (tr.getExitCode() != 0) score -= 100.0;

        // Big bonus for perfect repair
        if (tr.isAllPassed() && !tr.isTimedOut() && tr.getExitCode() == 0) {
            score += 10000.0;
        }

        return score;
    }

    private static int safeNonNeg(int x) {
        return Math.max(0, x);
    }
}
