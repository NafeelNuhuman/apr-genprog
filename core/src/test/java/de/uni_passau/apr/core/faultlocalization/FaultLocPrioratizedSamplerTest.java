package de.uni_passau.apr.core.faultlocalization;

import de.uni_passau.apr.core.patch.models.StatementId;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FaultLocPrioratizedSamplerTest {

    @TempDir
    Path tmp;

    @Test
    void ctor_whenFaultLocalizationHasNullStatements_throws() throws Exception {
        StatementCollector collector = collectorFrom(program());
        FaultLocalization fl = new FaultLocalization("A.java", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new FaultLocPrioratizedSampler(fl, collector, new Random(1)));
        assertTrue(ex.getMessage().contains("has no locations"));
    }

    @Test
    void ctor_whenFaultLocalizationHasEmptyStatements_throws() throws Exception {
        StatementCollector collector = collectorFrom(program());
        FaultLocalization fl = new FaultLocalization("A.java", List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new FaultLocPrioratizedSampler(fl, collector, new Random(1)));
        assertTrue(ex.getMessage().contains("has no locations"));
    }

    @Test
    void ctor_whenAllWeightsNonPositive_throwsDidntMapToAnyStatements() throws Exception {
        StatementCollector collector = collectorFrom(program());
        FaultLocalization fl = new FaultLocalization("A.java", List.of(
                new WeightedLocation(4, 0.0),
                new WeightedLocation(5, -1.0)
        ));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new FaultLocPrioratizedSampler(fl, collector, new Random(1)));
        assertTrue(ex.getMessage().contains("didnt map to any statements"));
    }

    @Test
    void getTarget_mapsContainedLine_toThatStatement() throws Exception {
        StatementCollector collector = collectorFrom(program());

        StatementId idIncrement = findIdContainingText(collector, "x++;");
        int line = idIncrement.beginLine();

        FaultLocalization fl = new FaultLocalization("A.java", List.of(
                new WeightedLocation(line, 1.0)
        ));

        FaultLocPrioratizedSampler sampler = new FaultLocPrioratizedSampler(fl, collector, new Random(1));
        assertEquals(idIncrement, sampler.getTarget());
    }

    @Test
    void getTarget_lineBeforeAllStatements_mapsToNextStatement() throws Exception {
        StatementCollector collector = collectorFrom(program());

        // choose a line before the method's first body statement; mapping should go to "next"
        int lineBefore = 1;

        FaultLocalization fl = new FaultLocalization("A.java", List.of(
                new WeightedLocation(lineBefore, 1.0)
        ));

        FaultLocPrioratizedSampler sampler = new FaultLocPrioratizedSampler(fl, collector, new Random(1));
        StatementId chosen = sampler.getTarget();

        assertNotNull(chosen);
        assertTrue(collector.allStatementIds().contains(chosen));
        assertTrue(chosen.beginLine() >= lineBefore + 1, "Should map to a statement after the given line");
    }

    @Test
    void getTarget_lineAfterAllStatements_mapsToPreviousStatement() throws Exception {
        StatementCollector collector = collectorFrom(program());

        int farAfter = 10_000;

        FaultLocalization fl = new FaultLocalization("A.java", List.of(
                new WeightedLocation(farAfter, 1.0)
        ));

        FaultLocPrioratizedSampler sampler = new FaultLocPrioratizedSampler(fl, collector, new Random(1));
        StatementId chosen = sampler.getTarget();

        assertNotNull(chosen);
        assertTrue(collector.allStatementIds().contains(chosen));
    }

    @Test
    void getTarget_respectsWeights_deterministicPickHitsHeavierBucket() throws Exception {
        StatementCollector collector = collectorFrom(program());

        StatementId idA = findIdContainingText(collector, "int a = 1;");
        StatementId idB = findIdContainingText(collector, "int b = 2;");

        FaultLocalization fl = new FaultLocalization("A.java", List.of(
                new WeightedLocation(idA.beginLine(), 1.0),
                new WeightedLocation(idB.beginLine(), 9.0)
        ));

        // nextDouble=0.95 => r=9.5 out of total=10 => should pick idB
        Random fixed = new FixedRandom(0.95);

        FaultLocPrioratizedSampler sampler = new FaultLocPrioratizedSampler(fl, collector, fixed);
        assertEquals(idB, sampler.getTarget());
    }

    @Test
    void buildCandidates_accumulatesWeights_thatMapToSameStatement() throws Exception {
        StatementCollector collector = collectorFrom(program());

        StatementId idA = findIdContainingText(collector, "int a = 1;");

        FaultLocalization fl = new FaultLocalization("A.java", List.of(
                new WeightedLocation(idA.beginLine(), 2.0),
                new WeightedLocation(idA.beginLine(), 3.0)
        ));

        FaultLocPrioratizedSampler sampler = new FaultLocPrioratizedSampler(fl, collector, new Random(1));
        assertEquals(idA, sampler.getTarget());
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

    private StatementId findIdContainingText(StatementCollector collector, String needle) {
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

    private static final class FixedRandom extends Random {
        private final double d;
        FixedRandom(double d) { this.d = d; }
        @Override public double nextDouble() { return d; }
    }
}
