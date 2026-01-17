package de.uni_passau.apr.core.selection;

import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.patch.models.*;

import java.util.List;
import java.util.Random;

/**
 * 1. Create tournament selection,
   2. Wrap in parent selector,
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
        TournamentSelection<Individual> ts = TournamentSelection.maximize(rng, 3, Individual::fitness);

        // Wrap it in ParentSelector
        ParentSelector<Individual> selector = pop -> ts.selectOne(pop);

        // Create NextGenerationProducer
        NextGenerationProducer<Individual> producer = new NextGenerationProducer<>(
                40,
                selector,            // selection strategy (tournament)
                Individual::patch,
                crossover,
                mutator,
                true,
                50                   // max attempts per child
        );

        return producer.produce(population);
    }
}
