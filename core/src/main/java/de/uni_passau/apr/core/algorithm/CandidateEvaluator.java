package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.patch.operators.PatchApplier;
import de.uni_passau.apr.core.patch.models.Patch;
import de.uni_passau.apr.core.testrunner.TestResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * 1. Converting a Patch into candidate source code (use PatchApplier)
 * 2. Running evaluation (use Evaluator)
 * 3. Computing a fitness score from TestResult
 *
 */
public final class CandidateEvaluator {

    private final Evaluator evaluator;

    // Fitness weights ( WPosT=1, WNegT=10)
    private final double wPosT;
    private final double wNegT;

    public CandidateEvaluator(Evaluator evaluator, double wPosT, double wNegT) {
        this.evaluator = Objects.requireNonNull(evaluator);
        if (wNegT <= 0) throw new IllegalArgumentException("wNegT must be > 0");
        this.wPosT = wPosT;
        this.wNegT = wNegT;
    }

    /**
     * Evaluates a list of patches
     */
    public List<EvaluatedCandidate> evaluateAll(BenchmarkConfig config, Path buggyFile, List<Patch> patches) throws Exception {
        Objects.requireNonNull(config);
        Objects.requireNonNull(buggyFile);
        Objects.requireNonNull(patches);

        List<EvaluatedCandidate> out = new ArrayList<>(patches.size());

        for (Patch patch : patches) {
            // apply patch -> candidate source
            String candidateSource;
            try {
                candidateSource = PatchApplier.apply(buggyFile, patch);
            } catch (Exception ex) {
                // Patch application failed -> treat as v bad candidate
                out.add(new EvaluatedCandidate(patch, Double.NEGATIVE_INFINITY, null));
                continue;
            }

            //evaluate candidate source (compile + run tests)
            EvaluationResult eval = evaluator.evaluate(config, candidateSource);

            //get fitness
            double fitness = computeFitness(eval);

            out.add(new EvaluatedCandidate(patch, fitness, eval));
        }

        return out;
    }

    /**
     * Returns true if the candidate is
     * considered (tests passed and not timed out) a successful repair.
     *
     */
    public boolean isSuccessful(EvaluatedCandidate cand) {
        if (cand == null || cand.evaluation() == null || cand.evaluation().getTestResult() == null) return false;
        TestResult tr = cand.evaluation().getTestResult();
        return tr.isAllPassed() && !tr.isTimedOut();
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
