package de.uni_passau.apr.core.selection;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.*;

class TournamentSelectionTest {

    @Test
    void ctor_nullRng_throws() {
        Comparator<Integer> cmp = Integer::compareTo;
        assertThrows(NullPointerException.class, () -> new TournamentSelection<>(null, 2, cmp));
    }

    @Test
    void ctor_tournamentSizeLessThan2_throws() {
        Random rng = new Random(1);
        Comparator<Integer> cmp = Integer::compareTo;

        assertThrows(IllegalArgumentException.class, () -> new TournamentSelection<>(rng, 0, cmp));
        assertThrows(IllegalArgumentException.class, () -> new TournamentSelection<>(rng, 1, cmp));
    }

    @Test
    void ctor_nullComparator_throws() {
        Random rng = new Random(1);
        assertThrows(NullPointerException.class, () -> new TournamentSelection<>(rng, 2, null));
    }

    @Test
    void selectOne_nullOrEmptyPopulation_throws() {
        TournamentSelection<Integer> sel = TournamentSelection.minimize(new Random(1), 2, x -> x);

        assertThrows(IllegalArgumentException.class, () -> sel.selectOne(null));
        assertThrows(IllegalArgumentException.class, () -> sel.selectOne(List.of()));
    }

    @Test
    void selectMany_countMustBePositive_throws() {
        TournamentSelection<Integer> sel = TournamentSelection.minimize(new Random(1), 2, x -> x);

        assertThrows(IllegalArgumentException.class, () -> sel.selectMany(List.of(1, 2), 0));
        assertThrows(IllegalArgumentException.class, () -> sel.selectMany(List.of(1, 2), -3));
    }

    @Test
    void selectMany_returnsExactlyCount_andAllMembersAreFromPopulation() {
        List<Integer> pop = List.of(10, 20, 30, 40);
        TournamentSelection<Integer> sel = TournamentSelection.minimize(new Random(123), 3, x -> x);

        List<Integer> parents = sel.selectMany(pop, 25);

        assertEquals(25, parents.size());
        for (Integer p : parents) {
            assertTrue(pop.contains(p), "Selected parent must be from population");
        }
    }

    @Test
    void minimize_selectOne_returnsAnElementThatIsNotWorseThanAnyDrawnCandidate_withFixedRng() {
        // We verify the selection logic deterministically by using a Random that returns a known index sequence.
        List<Integer> pop = List.of(5, 1, 9, 3); // smaller is better for minimize

        // Indices drawn: 0 -> 5, 2 -> 9, 3 -> 3  => best should become 3 (since min among 5,9,3 is 3)
        FixedRandom rng = new FixedRandom(new int[]{0, 2, 3}, new boolean[]{false});
        TournamentSelection<Integer> sel = TournamentSelection.minimize(rng, 3, x -> x);

        Integer chosen = sel.selectOne(pop);

        assertEquals(3, chosen);
    }

    @Test
    void maximize_selectOne_picksLargestAmongDrawnCandidates_withFixedRng() {
        List<Integer> pop = List.of(5, 1, 9, 3); // larger is better for maximize

        // Indices drawn: 1 -> 1, 0 -> 5, 3 -> 3  => best should be 5
        FixedRandom rng = new FixedRandom(new int[]{1, 0, 3}, new boolean[]{false});
        TournamentSelection<Integer> sel = TournamentSelection.maximize(rng, 3, x -> x);

        Integer chosen = sel.selectOne(pop);

        assertEquals(5, chosen);
    }

    @Test
    void tieBreak_cmpZero_canReplaceBest_whenNextBooleanTrue() {
        // Use objects where comparator always returns 0 -> tie every time.
        record Box(int id) {}

        List<Box> pop = List.of(new Box(1), new Box(2));

        // tournamentSize=2:
        // draw0 = pop[0] => best = Box(1)
        // draw1 = pop[1] => cmp==0 -> rng.nextBoolean() controls whether best becomes Box(2)
        FixedRandom rng = new FixedRandom(
                new int[]{0, 1},
                new boolean[]{true} // tie-break triggers replacement
        );

        TournamentSelection<Box> sel = new TournamentSelection<>(rng, 2, (a, b) -> 0);

        Box chosen = sel.selectOne(pop);
        assertEquals(2, chosen.id());
    }

    @Test
    void tieBreak_cmpZero_doesNotReplaceBest_whenNextBooleanFalse() {
        record Box(int id) {}

        List<Box> pop = List.of(new Box(1), new Box(2));

        FixedRandom rng = new FixedRandom(
                new int[]{0, 1},
                new boolean[]{false} // tie-break does NOT replace
        );

        TournamentSelection<Box> sel = new TournamentSelection<>(rng, 2, (a, b) -> 0);

        Box chosen = sel.selectOne(pop);
        assertEquals(1, chosen.id());
    }

    @Test
    void maximizeFactory_usesReversedComparator_highestFitnessPreferred() {
        record Box(double fitness) {}
        List<Box> pop = List.of(new Box(1.0), new Box(10.0), new Box(5.0));
        ToDoubleFunction<Box> f = Box::fitness;

        // draw sequence ensures all three are seen; best must be 10.0
        FixedRandom rng = new FixedRandom(new int[]{0, 2, 1}, new boolean[]{false});
        TournamentSelection<Box> sel = TournamentSelection.maximize(rng, 3, f);

        Box chosen = sel.selectOne(pop);
        assertEquals(10.0, chosen.fitness(), 0.0);
    }

    @Test
    void minimizeFactory_lowestFitnessPreferred() {
        record Box(double fitness) {}
        List<Box> pop = List.of(new Box(1.0), new Box(10.0), new Box(5.0));
        ToDoubleFunction<Box> f = Box::fitness;

        // draw sequence ensures all three are seen; best must be 1.0
        FixedRandom rng = new FixedRandom(new int[]{1, 2, 0}, new boolean[]{false});
        TournamentSelection<Box> sel = TournamentSelection.minimize(rng, 3, f);

        Box chosen = sel.selectOne(pop);
        assertEquals(1.0, chosen.fitness(), 0.0);
    }

    // ---------------------------------------------------------------------
    // Deterministic Random for tests
    // ---------------------------------------------------------------------
    private static final class FixedRandom extends Random {
        private final int[] nextInts;
        private int intPos = 0;

        private final boolean[] nextBools;
        private int boolPos = 0;

        FixedRandom(int[] nextInts, boolean[] nextBools) {
            this.nextInts = nextInts != null ? nextInts : new int[0];
            this.nextBools = nextBools != null ? nextBools : new boolean[0];
        }

        @Override
        public int nextInt(int bound) {
            assertTrue(bound > 0, "bound must be > 0");
            if (intPos >= nextInts.length) {
                throw new AssertionError("FixedRandom: not enough nextInt values supplied");
            }
            int v = nextInts[intPos++];
            // ensure returned value is in range [0, bound)
            int r = Math.floorMod(v, bound);
            return r;
        }

        @Override
        public boolean nextBoolean() {
            if (boolPos >= nextBools.length) {
                throw new AssertionError("FixedRandom: not enough nextBoolean values supplied");
            }
            return nextBools[boolPos++];
        }
    }
}
