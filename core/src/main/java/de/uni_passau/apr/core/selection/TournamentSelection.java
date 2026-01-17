package de.uni_passau.apr.core.selection;

import java.util.*;
import java.util.function.ToDoubleFunction;

public final class TournamentSelection<T> {

    private final Random rng;
    private final int tournamentSize;
    private final Comparator<T> comparator;

    /**
     * @param comparator Defines "better".
     * note - For maximizing fitness, use Comparator.comparingDouble(f).reversed()
     */
    public TournamentSelection(Random rng, int tournamentSize, Comparator<T> comparator) {
        this.rng = Objects.requireNonNull(rng);
        if (tournamentSize < 2) throw new IllegalArgumentException("tournament size must be >= 2");
        this.tournamentSize = tournamentSize;
        this.comparator = Objects.requireNonNull(comparator);
    }

    /** Select exactly one parent (with replacement). */
    public T selectOne(List<T> population) {
        if (population == null || population.isEmpty()) {
            throw new IllegalArgumentException("Population must not be empty");
        }

        T best = null;

        for (int i = 0; i < tournamentSize; i++) {
            T candidate = population.get(rng.nextInt(population.size()));
            if (best == null) {
                best = candidate;
            } else {
                int cmp = comparator.compare(candidate, best);
                if (cmp < 0) {
                    // comparator says candidate is better (cuz candidate < best in ordering)
                    best = candidate;
                } else if (cmp == 0 && rng.nextBoolean()) {
                    // diversity
                    best = candidate;
                }
            }
        }
        return best;
    }

    /** Select N parents (with replacements) */
    public List<T> selectMany(List<T> population, int count) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        List<T> parents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            parents.add(selectOne(population));
        }
        return parents;
    }

    /** Maximize numeric fitness (higher is better). */
    public static <T> TournamentSelection<T> maximize(Random rng, int tournamentSize, ToDoubleFunction<T> fitness) {
        Comparator<T> cmp = Comparator.comparingDouble(fitness).reversed();
        return new TournamentSelection<>(rng, tournamentSize, cmp);
    }

    /** Minimize numeric fitness */
    public static <T> TournamentSelection<T> minimize(Random rng, int tournamentSize, ToDoubleFunction<T> fitness) {
        Comparator<T> cmp = Comparator.comparingDouble(fitness);
        return new TournamentSelection<>(rng, tournamentSize, cmp);
    }
}
