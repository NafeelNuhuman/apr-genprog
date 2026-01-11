package de.uni_passau.apr.core.selection;

import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.patch.models.*;

import java.util.Comparator;

import java.util.List;
import java.util.Random;

/* 1. Create Tournament Selection,
   2. Wrap in Parent Selector,
   3. Produce next gen
 */
public final class NextGenerationProducerFactory {

    public static List<Patch> buildNextGenerationPatches(
            List<Individual> population,
            Random rng,
            SingleEditCrossover crossover,
            SingleEditMutator mutator
    ) {
        // Create TournamentSelection (higher f, better)
        TournamentSelection<Individual> ts = new TournamentSelection<>(
                rng,
                3, // tournament size k
                Comparator.comparingDouble(Individual::fitness).reversed()
        );

        // Wrap it in ParentSelector
        ParentSelector<Individual> selector = pop -> ts.selectOne(pop);

        // Create NextGenerationProducer
        NextGenerationProducer<Individual> producer = new NextGenerationProducer<>(
                40,     // population size
                selector,            // parent selection strategy (tournament)
                Individual::patch,   // how to extract Patch from Individual
                crossover,
                mutator,
                true,   // enforceUnique children
                50                   // max attempts per child
        );

        // Produce children patches
        return producer.produce(population);
    }
}
