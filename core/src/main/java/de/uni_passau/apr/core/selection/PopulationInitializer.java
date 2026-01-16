package de.uni_passau.apr.core.selection;

import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;

import java.util.*;

public final class PopulationInitializer {

    private final int populationSize;
    private final Random rng;
    private final StatementCollector collector;
    private final FaultLocPrioratizedSampler sampler;

    // 0.1 means 10% delete, 90% replace
    private final double deleteProbability;

    private final boolean sameTypeDonorOnly;

    // donor pool by statement class (IfStmt, ReturnStmt, ExpressionStmt, etc.)
    private final Map<Class<?>, List<StatementId>> donorsByType = new HashMap<>();

    public PopulationInitializer(int populationSize,
                                 Random rng,
                                 StatementCollector collector,
                                 FaultLocPrioratizedSampler sampler,
                                 double deleteProbability,
                                 boolean sameTypeDonorOnly) {
        if (populationSize <= 0) throw new IllegalArgumentException("populationSize must be > 0");
        if (deleteProbability < 0.0 || deleteProbability > 1.0) {
            throw new IllegalArgumentException("deleteProbability must be in [0,1]");
        }
        this.populationSize = populationSize;
        this.rng = Objects.requireNonNull(rng);
        this.collector = Objects.requireNonNull(collector);
        this.sampler = Objects.requireNonNull(sampler);
        this.deleteProbability = deleteProbability;
        this.sameTypeDonorOnly = sameTypeDonorOnly;

        buildDonorPools();
    }

    public List<Patch> initialize() {
        List<Patch> pop = new ArrayList<>(populationSize);
        Set<String> seen = new HashSet<>();

        int guard = 0;
        while (pop.size() < populationSize) {
            if (guard++ > populationSize * 50) {
                // safety
                break;
            }

            Patch p = createRandomSingleEditPatch();
            String sig = signature(p);

            // enforce uniqueness
            if (seen.add(sig)) {
                pop.add(p);
            }
        }

        // In case uniqueness blocked too much, fill remaining without uniqueness
        while (pop.size() < populationSize) {
            pop.add(createRandomSingleEditPatch());
        }

        return pop;
    }

    private Patch createRandomSingleEditPatch() {
        // 1 target biased by fault localization
        StatementId target = sampler.getTarget();

        // 2 choose operation type
        boolean doDelete = rng.nextDouble() < deleteProbability;

        if (doDelete) {
            return new Patch(List.of(new DeleteOp(target)));
        }

        // Replace, pick donor
        StatementId donor = pickDonorForTarget(target);

        // If we fail to find a donor, fallback to delete
        if (donor == null || donor.equals(target)) {
            return new Patch(List.of(new DeleteOp(target)));
        }

        return new Patch(List.of(new ReplaceOp(target, donor)));
    }

    private void buildDonorPools() {
        for (StatementId id : collector.allStatementIds()) {
            Class<?> type = collector.getStmt(id).getClass();
            donorsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(id);
        }
    }

    private StatementId pickDonorForTarget(StatementId target) {
        List<StatementId> all = collector.allStatementIds();
        if (all.isEmpty()) return null;

        if (!sameTypeDonorOnly) {
            // any statement as donor
            for (int i = 0; i < 20; i++) {
                StatementId d = all.get(rng.nextInt(all.size()));
                if (!d.equals(target)) return d;
            }
            return null;
        }

        // same type donors only
        Class<?> targetType = collector.getStmt(target).getClass();
        List<StatementId> candidates = donorsByType.getOrDefault(targetType, List.of());
        if (candidates.isEmpty()) return null;

        // pick random donor that isn't the target
        for (int i = 0; i < 20; i++) {
            StatementId d = candidates.get(rng.nextInt(candidates.size()));
            if (!d.equals(target)) return d;
        }

        // fallback scan
        for (StatementId d : candidates) {
            if (!d.equals(target)) return d;
        }
        return null;
    }

    private static String signature(Patch p) {
        // stableish uniqueness key
        EditOp op = p.edits().get(0);
        if (op instanceof DeleteOp d) {
            return "DEL@" + d.target();
        }
        ReplaceOp r = (ReplaceOp) op;
        return "REP@" + r.target() + "<-" + r.donor();
    }
}
