package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;
import de.uni_passau.apr.core.fitness.FitnessEvaluator;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.patch.operators.PatchApplier;
import de.uni_passau.apr.core.patch.models.Patch;
import de.uni_passau.apr.core.selection.PopulationInitializer;
import de.uni_passau.apr.core.patch.models.Individual;
import de.uni_passau.apr.core.selection.NextGenerationProducerFactory;
import de.uni_passau.apr.core.service.LoadedBenchmark;
import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.testrunner.TestResult;

import java.nio.file.Path;
import java.util.*;

/**
 * -build initial population
 * -evaluate population
 * -repeat generations (selection/crossover/mutation -> children)
 * -stop when repair found or limits reached
 */
public final class GenProgEngine implements RepairAlgorithm {

    private final FitnessEvaluator fitnessEvaluator;
    private final Evaluator evaluator;

    private final SingleEditCrossover crossover;
    private final SingleEditMutator mutator;

    private final int populationSize;
    private final int maxGenerations;

    public GenProgEngine(FitnessEvaluator fitnessEvaluator,
                         Evaluator evaluator,
                         SingleEditCrossover crossover,
                         SingleEditMutator mutator,
                         int populationSize,
                         int maxGenerations) {

        this.fitnessEvaluator = Objects.requireNonNull(fitnessEvaluator);
        this.evaluator = evaluator;
        this.crossover = Objects.requireNonNull(crossover);
        this.mutator = Objects.requireNonNull(mutator);

        if (populationSize <= 0) throw new IllegalArgumentException("population size must be > 0");
        if (maxGenerations <= 0) throw new IllegalArgumentException("max generations must be > 0");

        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
    }


    @Override
    public RepairResult run(LoadedBenchmark benchmark, RunConfig runConfig) {
        Objects.requireNonNull(benchmark, "benchmark");
        Objects.requireNonNull(runConfig, "runCnfig");

        InitContext ctx = initialisations(benchmark);

        BenchmarkConfig config = ctx.config();
        Path buggyFile = ctx.buggyFile();
        Random rand = ctx.rand();
        List<Patch> patches = ctx.patches();
        List<EvaluatedCandidate> population = ctx.population();
        EvaluatedCandidate bestSoFar = ctx.bestSoFar();

        //Evaluate initial population
        for (Patch patch : patches) {
            EvaluatedCandidate cand = evaluateCandidate(buggyFile, config, patch);
            population.add(cand);

            bestSoFar = updateBest(bestSoFar, cand);

            RepairResult success = successResultIfAny(buggyFile, cand);
            if (success != null) {
                return success;
            }
        }

        // If everything failed apply/evaluate, avoid NPE
        if (population.isEmpty() || bestSoFar == null) {
            return new RepairResult("", null, false);
        }

        //Generation eval loop
        for (int gen = 1; gen <= maxGenerations; gen++) {

            // Convert to selection individuals (Patch + fitness)
            List<Individual> selectionPop = toSelectionIndividuals(population);

            // Produce next generation patches (selection + crossover + mutation)
            List<Patch> childrenPatches = NextGenerationProducerFactory.buildNextGenerationPatches(
                    selectionPop, rand, crossover, mutator
            );

            List<EvaluatedCandidate> childPopulation = new ArrayList<>(childrenPatches.size());

            // Evaluate children
            for (Patch babyPatch : childrenPatches) {
                EvaluatedCandidate child = evaluateCandidate(buggyFile, config, babyPatch);
                childPopulation.add(child);

                bestSoFar = updateBest(bestSoFar, child);

                RepairResult success = successResultIfAny(buggyFile, child);
                if (success != null) {
                    return success;
                }
            }

            population = childPopulation;
        }


        //no full repair
        String bestCandidateSource;
        try {
            bestCandidateSource = PatchApplier.apply(buggyFile, bestSoFar.patch());
        } catch (Exception ex) {
            bestCandidateSource = "";
        }

        return new RepairResult(bestCandidateSource, bestSoFar.evaluation(), false);
    }

    //-------------Helpers----------------

    private RepairResult successResultIfAny(Path buggyFile, EvaluatedCandidate cand) {
        if (cand == null || cand.evaluation() == null || !isSuccessful(cand.evaluation())) return null;
        try {
            String src = PatchApplier.apply(buggyFile, cand.patch());
            return new RepairResult(src, cand.evaluation(), true);
        } catch (Exception e) {
            return new RepairResult("", cand.evaluation(), true);
        }
    }

    private EvaluatedCandidate evaluateCandidate(Path buggyFile, BenchmarkConfig config, Patch patch) {
        String candidateSource;
        try {
            candidateSource = PatchApplier.apply(buggyFile, patch);
        } catch (Exception ex) {
            return new EvaluatedCandidate(patch, Double.NEGATIVE_INFINITY, null);
        }

        EvaluationResult evalResult = evaluator.evaluate(config, candidateSource);
        double fitness = fitnessEvaluator.computeFitness(evalResult);
        return new EvaluatedCandidate(patch, fitness, evalResult);
    }

    private static EvaluatedCandidate updateBest(EvaluatedCandidate bestSoFar, EvaluatedCandidate cand) {
        if (cand == null) return bestSoFar;
        if (bestSoFar == null) return cand;
        return cand.fitness() > bestSoFar.fitness() ? cand : bestSoFar;
    }


    /**
     * Returns true if the candidate is
     * considered (tests passed and not timed out) a successful repair.
     */
    public boolean isSuccessful(EvaluationResult eval) {
        if (eval == null || eval.getTestResult() == null) return false;
        TestResult tr = eval.getTestResult();
        return tr.isAllPassed() && !tr.isTimedOut();
    }

    /**
     * Adapts evaluated candidates to the simple Individual(patch, fitness) used by selection.
     */
    private static List<Individual> toSelectionIndividuals(List<EvaluatedCandidate> pop) {
        List<Individual> out = new ArrayList<>(pop.size());
        for (EvaluatedCandidate c : pop) {
            out.add(new Individual(c.patch(), c.fitness()));
        }
        return out;
    }

    private record InitContext(
            BenchmarkConfig config,
            Path buggyFile,
            Random rand,
            List<Patch> patches,
            List<EvaluatedCandidate> population,
            EvaluatedCandidate bestSoFar
    ) {}

    private InitContext initialisations(LoadedBenchmark benchmark) {
        BenchmarkConfig config = benchmark.config();
        Path buggyFile = config.getBuggyProgramPath();

        Random rand = new Random();

        StatementCollector collector;
        try {
            collector = StatementCollector.fromFile(buggyFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var sampler = new FaultLocPrioratizedSampler(benchmark.faultLocalization(), collector, rand);

        PopulationInitializer initializer = new PopulationInitializer(
                populationSize,
                rand,
                collector,
                sampler,
                0.10,
                true
        );

        List<Patch> patches = initializer.initialize();
        List<EvaluatedCandidate> population = new ArrayList<>(patches.size());
        EvaluatedCandidate bestSoFar = null;

        return new InitContext(config, buggyFile, rand, patches, population, bestSoFar);
    }

}
