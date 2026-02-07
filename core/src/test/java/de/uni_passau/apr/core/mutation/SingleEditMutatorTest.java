package de.uni_passau.apr.core.mutation;

import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;
import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import de.uni_passau.apr.core.faultlocalization.WeightedLocation;
import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SingleEditMutatorTest {

    @TempDir
    Path tmp;

    @Test
    void ctor_probabilityOutOfRange_throws() throws Exception {
        StatementCollector collector = collectorFrom(program());
        FaultLocPrioratizedSampler sampler = samplerForLine(collector, anyId(collector).beginLine(), new ControlledRandom());

        assertThrows(IllegalArgumentException.class,
                () -> new SingleEditMutator(-0.1, new ControlledRandom(), collector, sampler, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SingleEditMutator(1.1, new ControlledRandom(), collector, sampler, false));
    }

    @Test
    void maybeMutate_whenNextDoubleLessOrEqualP_returnsOriginal_asImplemented() throws Exception {
        StatementCollector collector = collectorFrom(program());
        ControlledRandom rng = new ControlledRandom().withNextDouble(0.10); // <= p => returns original (current impl)

        FaultLocPrioratizedSampler sampler = samplerForLine(collector, anyId(collector).beginLine(), new ControlledRandom());

        SingleEditMutator mut = new SingleEditMutator(0.50, rng, collector, sampler, false);

        Patch original = new Patch(List.of(new DeleteOp(anyId(collector))));
        Patch out = mut.maybeMutate(original);

        assertSame(original, out);
    }

    @Test
    void mutateOnce_choice2_flipType_onReplaceOp_producesDeleteOp() throws Exception {
        StatementCollector collector = collectorFrom(program());

        StatementId target = idOfStatementContaining(collector, "int a = 1;");
        StatementId donor  = idOfStatementContaining(collector, "int b = 2;");
        Patch original = new Patch(List.of(new ReplaceOp(target, donor)));

        FaultLocPrioratizedSampler sampler = samplerForLine(collector, target.beginLine(), new ControlledRandom());

        ControlledRandom rng = new ControlledRandom()
                .withNextInt(2); // choice=2 => flipType

        SingleEditMutator mut = new SingleEditMutator(0.0, rng, collector, sampler, false);

        Patch out = mut.mutateOnce(original);

        assertEquals(1, out.edits().size());
        assertInstanceOf(DeleteOp.class, out.edits().get(0));
        assertEquals(target, ((DeleteOp) out.edits().get(0)).target());
    }

    @Test
    void mutateOnce_choice0_changeTarget_onDeleteOp_usesSamplerTarget() throws Exception {
        StatementCollector collector = collectorFrom(program());

        StatementId originalTarget = idOfStatementContaining(collector, "int a = 1;");
        StatementId samplerTarget  = idOfStatementContaining(collector, "int b = 2;");

        Patch original = new Patch(List.of(new DeleteOp(originalTarget)));

        // Sampler biased to samplerTarget's beginLine (single candidate, deterministic)
        FaultLocPrioratizedSampler sampler = samplerForLine(collector, samplerTarget.beginLine(), new ControlledRandom());

        ControlledRandom rng = new ControlledRandom()
                .withNextInt(0); // choice=0 => changeTarget

        SingleEditMutator mut = new SingleEditMutator(0.0, rng, collector, sampler, false);

        Patch out = mut.mutateOnce(original);

        assertEquals(1, out.edits().size());
        assertInstanceOf(DeleteOp.class, out.edits().get(0));
        assertEquals(samplerTarget, ((DeleteOp) out.edits().get(0)).target());
    }

    @Test
    void mutateOnce_whenAlwaysNullMutants_fallsBackToOriginal() throws Exception {
        StatementCollector collector = collectorFrom(program());

        StatementId target = anyId(collector);
        Patch original = new Patch(List.of(new DeleteOp(target)));

        FaultLocPrioratizedSampler sampler = samplerForLine(collector, target.beginLine(), new ControlledRandom());

        // For DeleteOp: choice=1 => changeDonor returns null. Do that 10 times => fallback.
        ControlledRandom rng = new ControlledRandom();
        for (int i = 0; i < 10; i++) rng.withNextInt(1);

        SingleEditMutator mut = new SingleEditMutator(0.0, rng, collector, sampler, false);

        Patch out = mut.mutateOnce(original);
        assertSame(original, out);
    }

    // ---------- helpers ----------

    private StatementCollector collectorFrom(String program) throws Exception {
        Path f = tmp.resolve("A.java");
        Files.writeString(f, program, StandardCharsets.UTF_8);
        return StatementCollector.fromFile(f);
    }

    private String program() {
        return """
                package t;
                public class A {
                  public int f(int x) {
                    int a = 1;
                    int b = 2;
                    if (x > 0) {
                      x++;
                    }
                    return a + b + x;
                  }
                }
                """;
    }

    private StatementId anyId(StatementCollector collector) {
        return collector.allStatementIds().get(0);
    }

    private StatementId idOfStatementContaining(StatementCollector collector, String needle) {
        for (StatementId id : collector.allStatementIds()) {
            String s = oneLine(collector.getStatement(id).toString());
            if (s.contains(needle)) return id;
        }
        fail("Could not find statement containing: " + needle);
        return null;
    }

    private String oneLine(String s) {
        return s.replace("\r", " ").replace("\n", " ").trim();
    }

    private FaultLocPrioratizedSampler samplerForLine(StatementCollector collector, int line, java.util.Random rand) {
        FaultLocalization fl = new FaultLocalization("A.java", List.of(new WeightedLocation(line, 1.0)));
        return new FaultLocPrioratizedSampler(fl, collector, rand);
    }

    /**
     * Queue-driven Random to force specific branches deterministically.
     */
    static final class ControlledRandom extends java.util.Random {
        private final Deque<Double> doubles = new ArrayDeque<>();
        private final Deque<Integer> ints = new ArrayDeque<>();

        ControlledRandom withNextDouble(double v) { doubles.addLast(v); return this; }
        ControlledRandom withNextInt(int v) { ints.addLast(v); return this; }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? 0.0 : doubles.removeFirst();
        }

        @Override
        public int nextInt(int bound) {
            int v = ints.isEmpty() ? 0 : ints.removeFirst();
            return Math.floorMod(v, bound);
        }
    }
}
