package de.uni_passau.apr.core.selection;

import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;
import de.uni_passau.apr.core.faultlocalization.WeightedLocation;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class NextGenerationProducerTest {

    @TempDir
    Path tempDir;

    // A small carrier type that already holds a patch.
    private record Cand(Patch patch) {}

    @Test
    void ctor_populationSizeMustBePositive() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(1));

        ParentSelector<Cand> selector = new RoundRobinSelector<>();
        Function<Cand, Patch> patchOf = Cand::patch;

        SingleEditCrossover crossover = new SingleEditCrossover(new Random(2), collector, false);
        SingleEditMutator mutator = new SingleEditMutator(0.0, new Random(3), collector, sampler, false);

        assertThrows(IllegalArgumentException.class, () ->
                new NextGenerationProducer<>(0, selector, patchOf, crossover, mutator, false, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new NextGenerationProducer<>(-1, selector, patchOf, crossover, mutator, false, 1));
    }

    @Test
    void ctor_nullArgs_throwNpe() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(1));

        ParentSelector<Cand> selector = new RoundRobinSelector<>();
        Function<Cand, Patch> patchOf = Cand::patch;
        SingleEditCrossover crossover = new SingleEditCrossover(new Random(2), collector, false);
        SingleEditMutator mutator = new SingleEditMutator(0.0, new Random(3), collector, sampler, false);

        assertThrows(NullPointerException.class, () ->
                new NextGenerationProducer<>(5, null, patchOf, crossover, mutator, false, 1));
        assertThrows(NullPointerException.class, () ->
                new NextGenerationProducer<>(5, selector, null, crossover, mutator, false, 1));
        assertThrows(NullPointerException.class, () ->
                new NextGenerationProducer<>(5, selector, patchOf, null, mutator, false, 1));
        assertThrows(NullPointerException.class, () ->
                new NextGenerationProducer<>(5, selector, patchOf, crossover, null, false, 1));
    }

    @Test
    void produce_nullOrEmptyPopulation_throws() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(1));

        NextGenerationProducer<Cand> prod = new NextGenerationProducer<>(
                5,
                new RoundRobinSelector<>(),
                Cand::patch,
                new SingleEditCrossover(new Random(2), collector, false),
                new SingleEditMutator(0.0, new Random(3), collector, sampler, false),
                false,
                1
        );

        assertThrows(IllegalArgumentException.class, () -> prod.produce(null));
        assertThrows(IllegalArgumentException.class, () -> prod.produce(List.of()));
    }

    @Test
    void produce_returnsExactlyPopulationSize_andEachChildIsSingleEditPatch() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(10));

        Random rng = new Random(11);
        SingleEditCrossover crossover = new SingleEditCrossover(rng, collector, false);
        // mutation prob 0 => stable, no null patches produced
        SingleEditMutator mutator = new SingleEditMutator(0.0, rng, collector, sampler, false);

        List<StatementId> ids = collector.allStatementIds();
        assertTrue(ids.size() >= 3);

        // population of candidate patches (single-edit)
        List<Cand> pop = List.of(
                new Cand(new Patch(List.of(new DeleteOp(ids.get(0))))),
                new Cand(new Patch(List.of(new ReplaceOp(ids.get(1), ids.get(2))))),
                new Cand(new Patch(List.of(new ReplaceOp(ids.get(2), ids.get(1)))))
        );

        NextGenerationProducer<Cand> prod = new NextGenerationProducer<>(
                12,
                new RoundRobinSelector<>(),
                Cand::patch,
                crossover,
                mutator,
                false,
                5
        );

        List<Patch> children = prod.produce(pop);

        assertEquals(12, children.size());
        for (Patch p : children) {
            assertNotNull(p);
            assertNotNull(p.edits());
            assertEquals(1, p.edits().size(), "Child patch must be single-edit");
            assertNotNull(p.edits().get(0));
        }
    }

    @Test
    void produce_enforceUnique_true_triesToAvoidDuplicateSignatures() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(20));

        Random rng = new Random(21);
        SingleEditCrossover crossover = new SingleEditCrossover(rng, collector, false);
        SingleEditMutator mutator = new SingleEditMutator(0.0, rng, collector, sampler, false);

        List<StatementId> ids = collector.allStatementIds();
        assertTrue(ids.size() >= 4);

        // Create parents that allow crossover to recombine into multiple different children
        // by having different targets/donors across parents.
        List<Cand> pop = List.of(
                new Cand(new Patch(List.of(new ReplaceOp(ids.get(0), ids.get(1))))),
                new Cand(new Patch(List.of(new ReplaceOp(ids.get(2), ids.get(3))))),
                new Cand(new Patch(List.of(new ReplaceOp(ids.get(1), ids.get(2))))),
                new Cand(new Patch(List.of(new ReplaceOp(ids.get(3), ids.get(0)))))
        );

        NextGenerationProducer<Cand> prod = new NextGenerationProducer<>(
                10,
                new RoundRobinSelector<>(),
                Cand::patch,
                crossover,
                mutator,
                true,
                10
        );

        List<Patch> children = prod.produce(pop);
        assertEquals(10, children.size());

        // Not guaranteed to be all unique (randomness + limited combos),
        // but we expect at least SOME uniqueness pressure.
        Set<String> sigs = new HashSet<>();
        for (Patch p : children) sigs.add(signature(p));

        assertTrue(sigs.size() >= 2, "With enforceUnique=true, expected at least some unique children");
    }

    @Test
    void produce_enforceUnique_true_withLowAttempts_canFallbackAndStillReachSize() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(30));

        // Use same seed so crossover tends to repeat (not guaranteed), and keep attempts tiny
        Random rng = new Random(31);
        SingleEditCrossover crossover = new SingleEditCrossover(rng, collector, false);
        SingleEditMutator mutator = new SingleEditMutator(0.0, rng, collector, sampler, false);

        List<StatementId> ids = collector.allStatementIds();
        assertTrue(ids.size() >= 2);

        // Highly duplicate-prone population: only two identical parent patches
        Patch parentPatch = new Patch(List.of(new ReplaceOp(ids.get(0), ids.get(1))));
        List<Cand> pop = List.of(new Cand(parentPatch), new Cand(parentPatch));

        NextGenerationProducer<Cand> prod = new NextGenerationProducer<>(
                8,
                new RoundRobinSelector<>(),
                Cand::patch,
                crossover,
                mutator,
                true,
                1 // forces quick fallback
        );

        List<Patch> children = prod.produce(pop);
        assertEquals(8, children.size(), "Must still produce full population size even if uniqueness blocks");
    }

    // ---------------- helpers ----------------

    /** Deterministic selector cycling through population. */
    private static final class RoundRobinSelector<E> implements ParentSelector<E> {
        private int i = 0;
        @Override
        public E selectOne(List<E> population) {
            E e = population.get(i % population.size());
            i++;
            return e;
        }
    }

    private Path writeJavaFile() throws Exception {
        String src = String.join("\n",
                "public class Program {",
                "  public int f(int x) {",
                "    int a = 0;",
                "    a = a + 1;",
                "    if (x > 0) {",
                "      a++;",
                "      a = a + 2;",
                "    } else {",
                "      a--;",
                "      a = a - 2;",
                "    }",
                "    return a;",
                "  }",
                "}"
        ) + "\n";

        Path file = tempDir.resolve("Program.java");
        Files.writeString(file, src, StandardCharsets.UTF_8);
        return file;
    }

    private FaultLocPrioratizedSampler newSampler(StatementCollector collector, Random rng) {
        List<StatementId> ids = collector.allStatementIds();
        assertFalse(ids.isEmpty());

        // use a few begin lines that certainly exist
        List<Integer> lines = ids.stream()
                .map(StatementId::beginLine)
                .distinct()
                .limit(5)
                .toList();

        List<WeightedLocation> wls = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            wls.add(new WeightedLocation(lines.get(i), (i % 2 == 0) ? 1.0 : 0.1));
        }

        FaultLocalization fl = new FaultLocalization("Program.java", wls);
        return new FaultLocPrioratizedSampler(fl, collector, rng);
    }

    private static String signature(Patch patch) {
        EditOp op = patch.edits().get(0);
        if (op instanceof DeleteOp d) return "DEL@" + d.target();
        if (op instanceof ReplaceOp r) return "REP@" + r.target() + "<-" + r.donor();
        return op.toString();
    }
}
