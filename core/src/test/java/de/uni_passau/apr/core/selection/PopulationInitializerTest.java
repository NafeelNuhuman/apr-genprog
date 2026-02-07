package de.uni_passau.apr.core.selection;

import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import de.uni_passau.apr.core.faultlocalization.WeightedLocation;
import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;
import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PopulationInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void ctor_invalidPopulationSize_throws() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(1));

        assertThrows(IllegalArgumentException.class, () ->
                new PopulationInitializer(0, new Random(1), collector, sampler, 0.1, true));
        assertThrows(IllegalArgumentException.class, () ->
                new PopulationInitializer(-5, new Random(1), collector, sampler, 0.1, true));
    }

    @Test
    void ctor_invalidDeleteProbability_throws() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(1));

        assertThrows(IllegalArgumentException.class, () ->
                new PopulationInitializer(5, new Random(1), collector, sampler, -0.01, true));
        assertThrows(IllegalArgumentException.class, () ->
                new PopulationInitializer(5, new Random(1), collector, sampler, 1.01, true));
    }

    @Test
    void ctor_nullArgs_throwNpe() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(1));

        assertThrows(NullPointerException.class, () ->
                new PopulationInitializer(5, null, collector, sampler, 0.1, true));
        assertThrows(NullPointerException.class, () ->
                new PopulationInitializer(5, new Random(1), null, sampler, 0.1, true));
        assertThrows(NullPointerException.class, () ->
                new PopulationInitializer(5, new Random(1), collector, null, 0.1, true));
    }

    @Test
    void initialize_returnsPopulationSize_andEachPatchHasSingleEdit() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(2));

        PopulationInitializer init = new PopulationInitializer(
                10, new Random(3), collector, sampler, 0.5, true
        );

        List<Patch> pop = init.initialize();

        assertEquals(10, pop.size());
        for (Patch p : pop) {
            assertNotNull(p);
            assertNotNull(p.edits());
            assertEquals(1, p.edits().size(), "Each patch should contain exactly one edit op");
            assertNotNull(p.edits().get(0));
        }
    }

    @Test
    void initialize_deleteProbabilityOne_producesOnlyDeleteOps() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(4));

        PopulationInitializer init = new PopulationInitializer(
                12, new Random(5), collector, sampler, 1.0, true
        );

        List<Patch> pop = init.initialize();

        assertEquals(12, pop.size());
        assertTrue(pop.stream().allMatch(p -> p.edits().get(0) instanceof DeleteOp),
                "With deleteProbability=1.0, all edits must be DeleteOp");
    }

    @Test
    void initialize_deleteProbabilityZero_producesMostlyReplaceOps_andNeverNullDonorInReplaceOp() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(6));

        PopulationInitializer init = new PopulationInitializer(
                20, new Random(7), collector, sampler, 0.0, false
        );

        List<Patch> pop = init.initialize();

        assertEquals(20, pop.size());

        long replaceCount = pop.stream().filter(p -> p.edits().get(0) instanceof ReplaceOp).count();
        long deleteCount = pop.stream().filter(p -> p.edits().get(0) instanceof DeleteOp).count();

        // Should be heavily biased to Replace; Delete only happens if donor==target or donor not found.
        assertTrue(replaceCount >= 10, "Expected mostly ReplaceOps when deleteProbability=0.0");
        assertEquals(20, replaceCount + deleteCount);

        for (Patch p : pop) {
            EditOp op = p.edits().get(0);
            if (op instanceof ReplaceOp r) {
                assertNotNull(r.donor());
                assertNotNull(r.target());
                assertNotEquals(r.target(), r.donor(), "ReplaceOp should not use target as donor");
            }
        }
    }

    @Test
    void initialize_sameTypeDonorOnly_true_donorTypeMatchesTargetType_forReplaceOps() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        FaultLocPrioratizedSampler sampler = newSampler(collector, new Random(8));

        PopulationInitializer init = new PopulationInitializer(
                30, new Random(9), collector, sampler, 0.0, true
        );

        List<Patch> pop = init.initialize();

        // Check only ReplaceOps (DeleteOps can happen rarely as fallback)
        List<ReplaceOp> replaces = pop.stream()
                .map(p -> p.edits().get(0))
                .filter(op -> op instanceof ReplaceOp)
                .map(op -> (ReplaceOp) op)
                .collect(Collectors.toList());

        assertFalse(replaces.isEmpty(), "Expected at least one ReplaceOp to validate donor type constraint");

        for (ReplaceOp r : replaces) {
            Class<?> targetType = stmtClass(collector, r.target());
            Class<?> donorType = stmtClass(collector, r.donor());
            assertEquals(targetType, donorType,
                    "When sameTypeDonorOnly=true, donor statement type must match target statement type");
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    /**
     * Builds a FaultLocPrioratizedSampler using a simple FaultLocalization object that references valid lines.
     * We give several weighted lines so sampler has options.
     */
    private FaultLocPrioratizedSampler newSampler(StatementCollector collector, Random rng) {
        // Choose a few statement begin lines from the collector to ensure they exist
        List<StatementId> ids = collector.allStatementIds();
        assertFalse(ids.isEmpty(), "Test Java file should produce statements");

        // Pick up to 5 distinct begin lines
        List<Integer> lines = ids.stream()
                .map(StatementId::beginLine)
                .distinct()
                .limit(5)
                .toList();

        List<WeightedLocation> wls = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            // alternate 1.0 and 0.1 weights
            double w = (i % 2 == 0) ? 1.0 : 0.1;
            wls.add(new WeightedLocation(lines.get(i), w));
        }

        FaultLocalization fl = new FaultLocalization("Program.java", wls);

        // This assumes your FaultLocPrioratizedSampler has the constructor used in RunCommand:
        // new FaultLocPrioratizedSampler(faultLocalization, statementCollector, random)
        return new FaultLocPrioratizedSampler(fl, collector, rng);
    }

    private Path writeJavaFile() throws Exception {
        // Keep it statement-rich and with repeated statement types (ExpressionStmt etc.)
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

    /**
     * Your code sometimes uses collector.getStatement(id) while earlier you had getStmt(id).
     * This helper supports both names without assuming which one exists.
     */
    private static Class<?> stmtClass(StatementCollector collector, StatementId id) throws Exception {
        Object stmt = invokeCollector(collector, "getStatement", id);
        if (stmt == null) stmt = invokeCollector(collector, "getStmt", id);
        if (stmt == null) {
            throw new AssertionError("StatementCollector has neither getStatement(StatementId) nor getStmt(StatementId)");
        }
        return stmt.getClass();
    }

    private static Object invokeCollector(StatementCollector collector, String method, StatementId id) {
        try {
            var m = collector.getClass().getMethod(method, StatementId.class);
            return m.invoke(collector, id);
        } catch (Exception e) {
            return null;
        }
    }
}
