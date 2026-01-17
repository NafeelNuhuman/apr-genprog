package de.uni_passau.apr.core.selection;

import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.patch.models.*;

import java.util.*;
import java.util.function.Function;

public final class NextGenerationProducer<E> {

    private final int populationSize;
    private final ParentSelector<E> selector;
    private final Function<E, Patch> patchOf;

    private final SingleEditCrossover crossover;
    private final SingleEditMutator mutator;

    private final boolean enforceUnique;
    private final int maxAttemptsPerChild;

    public NextGenerationProducer(int populationSize,
                                  ParentSelector<E> selector,
                                  Function<E, Patch> patchOf,
                                  SingleEditCrossover crossover,
                                  SingleEditMutator mutator,
                                  boolean enforceUnique,
                                  int maxAttemptsPerChild) {
        if (populationSize <= 0) throw new IllegalArgumentException("population size must be > 0");
        this.populationSize = populationSize;
        this.selector = Objects.requireNonNull(selector);
        this.patchOf = Objects.requireNonNull(patchOf);
        this.crossover = Objects.requireNonNull(crossover);
        this.mutator = Objects.requireNonNull(mutator);
        this.enforceUnique = enforceUnique;
        this.maxAttemptsPerChild = Math.max(1, maxAttemptsPerChild);
    }

    /**
     * Build next generation patches from an already
     * evaluated population and return child patches.
     */
    public List<Patch> produce(List<E> population) {
        if (population == null || population.isEmpty()) {
            throw new IllegalArgumentException("population must not be empty");
        }

        List<Patch> children = new ArrayList<>(populationSize);
        Set<String> seen = enforceUnique ? new HashSet<>() : null;

        int globalGuard = 0;
        while (children.size() < populationSize) {
            if (++globalGuard > populationSize * maxAttemptsPerChild) {
                if (enforceUnique) {
                    return fillWithoutUniqueness(children, population);
                }
                break;
            }

            E p1 = selector.selectOne(population);
            E p2 = selector.selectOne(population);

            Patch parent1 = patchOf.apply(p1);
            Patch parent2 = patchOf.apply(p2);

            Patch child = crossover.crossover(parent1, parent2);
            child = mutator.maybeMutate(child);

            if (child == null) continue;

            if (enforceUnique) {
                String sig = signatureSingleEdit(child);
                if (!seen.add(sig)) continue;
            }

            children.add(child);
        }

        // If we didn’t reach size, fill by cloning parent patches
        while (children.size() < populationSize) {
            E p = selector.selectOne(population);
            children.add(patchOf.apply(p));
        }

        return children;
    }

    private List<Patch> fillWithoutUniqueness(List<Patch> already, List<E> population) {
        List<Patch> children = new ArrayList<>(already);
        while (children.size() < populationSize) {
            E p1 = selector.selectOne(population);
            E p2 = selector.selectOne(population);
            Patch child = crossover.crossover(patchOf.apply(p1), patchOf.apply(p2));
            child = mutator.maybeMutate(child);
            if (child != null) children.add(child);
        }
        return children;
    }

    private static String signatureSingleEdit(Patch patch) {
        //Using only 1 edit
        EditOp op = patch.edits().get(0);

        if (op instanceof DeleteOp d) {
            return "DEL@" + d.target();
        }
        if (op instanceof ReplaceOp r) {
            return "REP@" + r.target() + "<-" + r.donor();
        }
        return op.toString();
    }
}
