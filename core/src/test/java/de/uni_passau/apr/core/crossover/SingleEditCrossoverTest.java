package de.uni_passau.apr.core.crossover;

import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.patch.operators.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SingleEditCrossoverTest {

    @TempDir
    Path tempDir;

    @Test
    void ctor_nullArgs_throwNpe() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());

        assertThrows(NullPointerException.class, () -> new SingleEditCrossover(null, collector, false));
        assertThrows(NullPointerException.class, () -> new SingleEditCrossover(new Random(1), null, false));
    }

    @Test
    void crossover_nullParents_throwNpe() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        SingleEditCrossover cross = new SingleEditCrossover(new Random(1), collector, false);

        Patch p = new Patch(List.of(new DeleteOp(anyId(collector))));
        assertThrows(NullPointerException.class, () -> cross.crossover(null, p));
        assertThrows(NullPointerException.class, () -> cross.crossover(p, null));
    }

    @Test
    void crossover_requiresSingleEditPatch_throwsIfMultipleEdits() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());
        SingleEditCrossover cross = new SingleEditCrossover(new Random(1), collector, false);

        Patch multi = new Patch(List.of(
                new DeleteOp(anyId(collector)),
                new DeleteOp(anyOtherId(collector))
        ));

        // both parents invalid
        assertThrows(IllegalArgumentException.class, () -> cross.crossover(multi, multi));

        // one invalid parent is enough (it evaluates both p1 and p2)
        Patch single = new Patch(List.of(new DeleteOp(anyId(collector))));
        assertThrows(IllegalArgumentException.class, () -> cross.crossover(multi, single));
        assertThrows(IllegalArgumentException.class, () -> cross.crossover(single, multi));
    }


    @Test
    void crossover_whenNotBothReplace_inheritsOneParent_basedOnRngBoolean() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());

        // rng.nextBoolean() ? p1 : p2  -> make it false so we get p2
        FixedRandom rng = new FixedRandom(new boolean[]{false}, new int[]{});
        SingleEditCrossover cross = new SingleEditCrossover(rng, collector, false);

        Patch p1 = new Patch(List.of(new DeleteOp(anyId(collector))));
        Patch p2 = new Patch(List.of(new ReplaceOp(anyId(collector), anyOtherId(collector))));

        Patch child = cross.crossover(p1, p2);
        assertSame(p2, child, "When not both are ReplaceOps, should return one parent; here expected p2");
    }

    @Test
    void crossover_whenBothReplace_recombinesTargetAndDonor_asSpecifiedByRng() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());

        // recombineReplace:
        // target = rng.nextBoolean() ? ra.target : rb.target
        // We'll force true => choose ra.target, and donor from rb
        FixedRandom rng = new FixedRandom(new boolean[]{true}, new int[]{});
        SingleEditCrossover cross = new SingleEditCrossover(rng, collector, false);

        StatementId tA = findIdByStmtSimpleName(collector, "IfStmt");       // target from A
        StatementId dA = findIdByStmtSimpleName(collector, "ReturnStmt");   // donor in A
        StatementId tB = findAnotherIdByStmtSimpleName(collector, "IfStmt", tA); // other if as target in B
        StatementId dB = findIdByStmtSimpleName(collector, "ExpressionStmt");   // donor in B

        Patch p1 = new Patch(List.of(new ReplaceOp(tA, dA)));
        Patch p2 = new Patch(List.of(new ReplaceOp(tB, dB)));

        Patch child = cross.crossover(p1, p2);
        assertNotNull(child);
        assertEquals(1, child.edits().size());
        assertTrue(child.edits().get(0) instanceof ReplaceOp);

        ReplaceOp rop = (ReplaceOp) child.edits().get(0);

        // target chosen from A; donor should come from B (since target==ra.target)
        assertEquals(tA, rop.target());
        assertEquals(dB, rop.donor());
    }

    @Test
    void crossover_whenDonorEqualsTarget_fixesDonor_viaPickCompatibleDonor() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());

        // Force target selection: choose ra.target (true)
        // Then donor from other parent equals target => triggers pickCompatibleDonor().
        // In !sameType mode, pickCompatibleDonor picks a random statement; if it's target, it returns all.get(0).
        // We’ll force nextInt to point at the target, so it falls back to all.get(0) which should not be the target.
        StatementId target = findIdByStmtSimpleName(collector, "IfStmt");
        StatementId someOther = findIdByStmtSimpleName(collector, "ReturnStmt");

        FixedRandom rng = new FixedRandom(
                new boolean[]{true},              // choose ra.target
                new int[]{indexOf(collector, target)} // pickCompatibleDonor picks target -> fallback to all.get(0)
        );

        SingleEditCrossover cross = new SingleEditCrossover(rng, collector, false);

        // Make rb.donor == target so donor==target
        Patch p1 = new Patch(List.of(new ReplaceOp(target, someOther)));
        Patch p2 = new Patch(List.of(new ReplaceOp(someOther, target)));

        Patch child = cross.crossover(p1, p2);
        ReplaceOp rop = (ReplaceOp) child.edits().get(0);

        assertEquals(target, rop.target());
        assertNotEquals(target, rop.donor(), "Donor must be fixed if it equals target");
    }

    @Test
    void crossover_sameTypeDonorOnly_true_repairsMismatchedDonorToSameStatementType() throws Exception {
        StatementCollector collector = StatementCollector.fromFile(writeJavaFile());

        // Need target type IfStmt, and donor from other parent of different type (ReturnStmt),
        // so sameTypeDonorOnly triggers pickCompatibleDonor to find another IfStmt donor.
        StatementId if1 = findIdByStmtSimpleName(collector, "IfStmt");
        StatementId if2 = findAnotherIdByStmtSimpleName(collector, "IfStmt", if1);
        StatementId ret = findIdByStmtSimpleName(collector, "ReturnStmt");

        // Force target selection to choose ra.target (=if1)
        // Then donor initially from rb.donor (=ret) => mismatch => pickCompatibleDonor().
        // Force nextInt to point to if2 as a compatible donor.
        int if2Index = indexOf(collector, if2);
        FixedRandom rng = new FixedRandom(
                new boolean[]{true},     // pick ra.target
                new int[]{if2Index}      // pickCompatibleDonor picks if2 (same type as if1)
        );

        SingleEditCrossover cross = new SingleEditCrossover(rng, collector, true);

        Patch p1 = new Patch(List.of(new ReplaceOp(if1, if2)));   // ra
        Patch p2 = new Patch(List.of(new ReplaceOp(ret, ret)));   // rb (rb.donor is ReturnStmt)

        Patch child = cross.crossover(p1, p2);
        ReplaceOp rop = (ReplaceOp) child.edits().get(0);

        assertEquals(if1, rop.target());
        assertNotEquals(if1, rop.donor());

        String targetType = stmtSimpleName(collector, rop.target());
        String donorType = stmtSimpleName(collector, rop.donor());
        assertEquals(targetType, donorType, "When sameTypeDonorOnly=true, donor must match target statement type");
        assertEquals("IfStmt", targetType);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Path writeJavaFile() throws Exception {
        // Two if statements so we can get two IfStmt donors/targets
        String src = String.join("\n",
                "public class Program {",
                "  public int f(int x) {",
                "    int a = 0;",
                "    a = a + 1;",
                "    if (x > 0) {",
                "      a++;",
                "    }",
                "    if (x < 0) {",
                "      a--;",
                "    }",
                "    return a;",
                "  }",
                "}"
        ) + "\n";

        Path file = tempDir.resolve("Program.java");
        Files.writeString(file, src, StandardCharsets.UTF_8);
        return file;
    }

    private static StatementId anyId(StatementCollector c) {
        List<StatementId> ids = c.allStatementIds();
        assertFalse(ids.isEmpty());
        return ids.get(0);
    }

    private static StatementId anyOtherId(StatementCollector c) {
        List<StatementId> ids = c.allStatementIds();
        assertTrue(ids.size() >= 2);
        return ids.get(1);
    }

    private static int indexOf(StatementCollector c, StatementId id) {
        List<StatementId> ids = c.allStatementIds();
        int idx = ids.indexOf(id);
        assertTrue(idx >= 0, "StatementId must be in collector");
        return idx;
    }

    private static StatementId findIdByStmtSimpleName(StatementCollector c, String simpleName) throws Exception {
        for (StatementId id : c.allStatementIds()) {
            Object stmt = c.getStatement(id);
            if (stmt != null && stmt.getClass().getSimpleName().equals(simpleName)) {
                return id;
            }
        }
        throw new AssertionError("Could not find a statement of type " + simpleName + " in test file");
    }

    private static StatementId findAnotherIdByStmtSimpleName(StatementCollector c, String simpleName, StatementId notThis) throws Exception {
        for (StatementId id : c.allStatementIds()) {
            if (id.equals(notThis)) continue;
            Object stmt = c.getStatement(id);
            if (stmt != null && stmt.getClass().getSimpleName().equals(simpleName)) {
                return id;
            }
        }
        throw new AssertionError("Could not find a second statement of type " + simpleName + " in test file");
    }

    private static String stmtSimpleName(StatementCollector c, StatementId id) throws Exception {
        Object stmt = c.getStatement(id);
        assertNotNull(stmt, "collector.getStatement(id) returned null");
        return stmt.getClass().getSimpleName();
    }

    /**
     * Deterministic Random that feeds nextBoolean()/nextInt() from provided sequences.
     */
    private static final class FixedRandom extends Random {
        private final boolean[] bools;
        private int bi = 0;

        private final int[] ints;
        private int ii = 0;

        FixedRandom(boolean[] bools, int[] ints) {
            this.bools = (bools == null) ? new boolean[0] : bools;
            this.ints = (ints == null) ? new int[0] : ints;
        }

        @Override
        public boolean nextBoolean() {
            if (bi >= bools.length) {
                throw new AssertionError("FixedRandom: not enough booleans supplied");
            }
            return bools[bi++];
        }

        @Override
        public int nextInt(int bound) {
            if (ii >= ints.length) {
                throw new AssertionError("FixedRandom: not enough ints supplied");
            }
            int v = ints[ii++];
            return Math.floorMod(v, bound);
        }
    }
}
