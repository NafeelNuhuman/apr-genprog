package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.faultlocalization.FaultLocGuidedSampler;
import de.uni_passau.apr.core.faultlocalization.FaultLocalizationProvider;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.patch.models.Patch;
import de.uni_passau.apr.core.selection.PopulationInitializer;
import de.uni_passau.apr.core.patch.models.Individual;
import de.uni_passau.apr.core.selection.NextGenerationProducerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * -build initial population
 * -evaluate population
 * -repeat generations (selection/crossover/mutation -> children)
 * -stop when repair found or limits reached
 */
public final class GenProgEngine {

    private final FaultLocalizationProvider faultLocProvider;
    private final CandidateEvaluator candidateEvaluator;

    private final de.uni_passau.apr.core.crossover.SingleEditCrossover crossover;
    private final de.uni_passau.apr.core.mutation.SingleEditMutator mutator;

    private final int populationSize;
    private final int maxGenerations;

    public GenProgEngine(FaultLocalizationProvider faultLocProvider,
                         CandidateEvaluator candidateEvaluator,
                         de.uni_passau.apr.core.crossover.SingleEditCrossover crossover,
                         de.uni_passau.apr.core.mutation.SingleEditMutator mutator,
                         int populationSize,
                         int maxGenerations) {

        this.faultLocProvider = Objects.requireNonNull(faultLocProvider);
        this.candidateEvaluator = Objects.requireNonNull(candidateEvaluator);
        this.crossover = Objects.requireNonNull(crossover);
        this.mutator = Objects.requireNonNull(mutator);

        if (populationSize <= 0) throw new IllegalArgumentException("populationSize must be > 0");
        if (maxGenerations <= 0) throw new IllegalArgumentException("maxGenerations must be > 0");

        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
    }

    /**
     * Output of the search run.
     * - repaired=true means we found a patch that passes all tests
     * - bestCandidate always holds the best seen (even if no full repair was found)
     */
    public record SearchResult(boolean repaired, EvaluatedCandidate bestCandidate, int generationsRan) { }

    /**
     * Runs the GenProg loop for a single benchmark.
     *
     * @param config benchmark config used by faultloc provider + evaluator
     * @param buggyFile path to buggy Java file (needed for parsing + patch apply)
     * @param rng random generator (seeded externally)
     */
    public SearchResult repair(BenchmarkConfig config, Path buggyFile, Random rng) throws Exception {
        Objects.requireNonNull(config);
        Objects.requireNonNull(buggyFile);
        Objects.requireNonNull(rng);

        //Parse buggy file into statements
        StatementCollector collector = StatementCollector.fromFile(buggyFile);

        //Build faultloc guided sampler
        var sampler = new FaultLocGuidedSampler(
                config, faultLocProvider, collector, rng
        );

        //Initial population of random single-edit patches
        PopulationInitializer initializer = new PopulationInitializer(
                populationSize,
                rng,
                collector,
                sampler,
                0.10, // deleteProbability
                true  // sameTypeDonorOnly
        );

        List<Patch> patches = initializer.initialize();

        //Evaluate initial population
        List<EvaluatedCandidate> population = candidateEvaluator.evaluateAll(config, buggyFile, patches);
        EvaluatedCandidate bestSoFar = bestOf(population);

        if (candidateEvaluator.isSuccessful(bestSoFar)) {
            return new SearchResult(true, bestSoFar, 0);
        }

        //Evolution loop
        int gen;
        for (gen = 1; gen <= maxGenerations; gen++) {

            // Convert to selection individuals (Patch + fitness)
            List<Individual> selectionPop = toSelectionIndividuals(population);

            // Produce next generation patches (selection -> crossover -> mutation)
            List<Patch> childrenPatches = NextGenerationProducerFactory.buildNextGenerationPatches(
                    selectionPop, rng, crossover, mutator
            );

            // Evaluate children
            List<EvaluatedCandidate> children = candidateEvaluator.evaluateAll(config, buggyFile, childrenPatches);

            // Success check
            for (EvaluatedCandidate c : children) {
                if (candidateEvaluator.isSuccessful(c)) {
                    return new SearchResult(true, c, gen);
                }
            }

            // Best-so-far tracking
            EvaluatedCandidate bestChild = bestOf(children);
            if (bestChild.fitness() > bestSoFar.fitness()) {
                bestSoFar = bestChild;
            }

            population = children;
        }

        // No full repair found
        return new SearchResult(false, bestSoFar, maxGenerations);
    }

    /**
     * Returns the best candidate by fitness (higher is better).
     */
    private static EvaluatedCandidate bestOf(List<EvaluatedCandidate> pop) {
        return pop.stream()
                .max(Comparator.comparingDouble(EvaluatedCandidate::fitness))
                .orElseThrow(() -> new IllegalArgumentException("empty population"));
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
