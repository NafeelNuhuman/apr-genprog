package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.fitness.FitnessEvaluator;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
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

    private final PopulationInitializer populationInitializer;
    private final FitnessEvaluator fitnessEvaluator;
    private final Evaluator evaluator;

    private final SingleEditCrossover crossover;
    private final SingleEditMutator mutator;

    public GenProgEngine(PopulationInitializer populationInitializer,
                         FitnessEvaluator fitnessEvaluator,
                         Evaluator evaluator,
                         SingleEditCrossover crossover,
                         SingleEditMutator mutator) {
        this.populationInitializer = Objects.requireNonNull(populationInitializer);
        this.fitnessEvaluator = Objects.requireNonNull(fitnessEvaluator);
        this.evaluator = Objects.requireNonNull(evaluator);
        this.crossover = Objects.requireNonNull(crossover);
        this.mutator = Objects.requireNonNull(mutator);
    }


    @Override
    public RepairResult run(LoadedBenchmark benchmark, RunConfig runConfig) {
        Objects.requireNonNull(benchmark, "benchmark");
        Objects.requireNonNull(runConfig, "runCnfig");

        System.out.println("Starting GenProgEngine on benchmark: " + benchmark.config().getName() +
                " with max generations: " + runConfig.maxGenerations() +
                " and population size: " + runConfig.populationSize() + ".");

        BenchmarkConfig config = benchmark.config();
        Path buggyFile = benchmark.config().getBuggyProgramPath();
        Random rand = runConfig.random() != null ? runConfig.random() : new Random();
        List<Patch> patches = populationInitializer.initialize();
        List<EvaluatedCandidate> population = new ArrayList<>(patches.size());
        System.out.println("Initialized population with " + patches.size() + " patches.");
        EvaluatedCandidate bestSoFar = null;

        //Evaluate initial population
        int idx = 0;
        for (Patch patch : patches) {
            EvaluatedCandidate cand = evaluateCandidate(buggyFile, config, patch);
            System.out.println("Evaluated initial population candidate " + idx + " with fitness: " + cand.fitness());
            population.add(cand);

            bestSoFar = updateBest(bestSoFar, cand);
            idx++;

            RepairResult success = successResultIfAny(buggyFile, cand);
            if (success != null) {
                System.out.println("Found successful repair in initial population.");
                return success;
            }
        }
        System.out.println("Initial population evaluation complete. Best fitness so far: " +
                (bestSoFar != null ? bestSoFar.fitness() : "N/A"));

        // If everything failed apply/evaluate, avoid NPE
        if (population.isEmpty() || bestSoFar == null) {
            return new RepairResult("", null, false);
        }

        //Generation eval loop
        for (int gen = 1; gen <= runConfig.maxGenerations(); gen++) {
            System.out.println("Generation " + gen + " started. Best fitness so far: " + bestSoFar.fitness());

            // Convert to selection individuals (Patch + fitness)
            List<Individual> selectionPop = toSelectionIndividuals(population);
            System.out.println("Selection population prepared with " + selectionPop.size() + " individuals.");

            // Produce next generation patches (selection + crossover + mutation)
            List<Patch> childrenPatches = NextGenerationProducerFactory.buildNextGenerationPatches(
                    selectionPop, rand, crossover, mutator
            );
            System.out.println("Produced " + childrenPatches.size() + " children patches for generation " + gen + ".");

            List<EvaluatedCandidate> childPopulation = new ArrayList<>(childrenPatches.size());

            int compiledCandidates = 0;
            int compiledFailures = 0;
            // Evaluate children
            for (Patch babyPatch : childrenPatches) {
                EvaluatedCandidate child = evaluateCandidate(buggyFile, config, babyPatch);
                if (child.evaluation() != null) {
                    if (child.evaluation().getTestResult().getTestsRun() > 0) {
                        compiledCandidates++;
                    } else if (child.evaluation().getTestResult().getExitCode() != 0) {
                        compiledFailures++;
                    }
                }

                System.out.println("Evaluated child candidate with fitness: " + child.fitness());
                childPopulation.add(child);

                bestSoFar = updateBest(bestSoFar, child);

                RepairResult success = successResultIfAny(buggyFile, child);
                if (success != null) {
                    return success;
                }
            }
            System.out.println("Generation " + gen + " evaluation complete. \nCompiled candidates: "
                    + compiledCandidates + ", \nCompile failures: " + compiledFailures +
                    ". \nBest fitness so far: " + bestSoFar.fitness());

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
            return new EvaluatedCandidate(patch, -1e15, null);
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
}
