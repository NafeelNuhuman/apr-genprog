package de.uni_passau.apr.core.crossover;

import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.patch.models.*;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class SingleEditCrossover {

    private final Random rng;
    private final StatementCollector collector;
    private final boolean sameTypeDonorOnly;

    public SingleEditCrossover(Random rng, StatementCollector collector, boolean sameTypeDonorOnly) {
        this.rng = Objects.requireNonNull(rng);
        this.collector = Objects.requireNonNull(collector);
        this.sameTypeDonorOnly = sameTypeDonorOnly;
    }

    public Patch crossover(Patch p1, Patch p2) {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);

        EditOp a = singleOp(p1);
        EditOp b = singleOp(p2);

        // If both are ReplaceOps, we can recombine target/donor
        if (a instanceof ReplaceOp ra && b instanceof ReplaceOp rb) {
            ReplaceOp child = recombineReplace(ra, rb);
            return new Patch(List.of(child));
        }

        // Otherwise, inherit one parent's single edit
        return rng.nextBoolean() ? p1 : p2;
    }

    private ReplaceOp recombineReplace(ReplaceOp ra, ReplaceOp rb) {
        // 50/50 which parent provides target
        StatementId target = rng.nextBoolean() ? ra.target() : rb.target();
        // donor from the other parent
        StatementId donor = (target.equals(ra.target())) ? rb.donor() : ra.donor();

        // Validate / fix donor if needed
        if (donor.equals(target) || (sameTypeDonorOnly && !sameType(target, donor))) {
            donor = pickCompatibleDonor(target);
        }
        return new ReplaceOp(target, donor);
    }

    private boolean sameType(StatementId t, StatementId d) {
        return collector.getStatement(t).getClass().equals(collector.getStatement(d).getClass());
    }

    private StatementId pickCompatibleDonor(StatementId target) {
        var all = collector.allStatementIds();
        if (all.isEmpty()) return target; // worst case scenario (will be filtered later)

        if (!sameTypeDonorOnly) {
            StatementId donor = all.get(rng.nextInt(all.size()));
            return donor.equals(target) ? all.get(0) : donor;
        }

        Class<?> targetType = collector.getStatement(target).getClass();
        for (int i = 0; i < 30; i++) {
            StatementId candidate = all.get(rng.nextInt(all.size()));
            if (!candidate.equals(target) && collector.getStatement(candidate).getClass().equals(targetType)) {
                return candidate;
            }
        }
        // fallback scan
        for (StatementId candidate : all) {
            if (!candidate.equals(target) && collector.getStatement(candidate).getClass().equals(targetType)) {
                return candidate;
            }
        }
        // last resort
        return all.get(0);
    }

    private static EditOp singleOp(Patch patch) {
        List<EditOp> edits = patch.edits();
        if (edits == null || edits.size() != 1) {
            throw new IllegalArgumentException("Expected single-edit patch, got: " + (edits == null ? 0 : edits.size()));
        }
        return edits.get(0);
    }
}
