package de.uni_passau.apr.core.mutation;

import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.faultlocalization.FaultLocPrioratizedSampler;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class SingleEditMutator {

    private final double mutationProbability;
    private final Random rng;
    private final StatementCollector collector;
    private final FaultLocPrioratizedSampler sampler;
    private final boolean sameTypeDonorOnly;

    public SingleEditMutator(double mutationProbability,
                             Random rng,
                             StatementCollector collector,
                             FaultLocPrioratizedSampler sampler,
                             boolean sameTypeDonorOnly) {
        if (mutationProbability < 0.0 || mutationProbability > 1.0) {
            throw new IllegalArgumentException("mutation probability must be in [0,1]");
        }
        this.mutationProbability = mutationProbability;
        this.rng = Objects.requireNonNull(rng);
        this.collector = Objects.requireNonNull(collector);
        this.sampler = Objects.requireNonNull(sampler);
        this.sameTypeDonorOnly = sameTypeDonorOnly;
    }

    // Apply mutation with probability p; otherwise return the original patch unchanged.
    public Patch maybeMutate(Patch patch) {
        Objects.requireNonNull(patch);
        if (rng.nextDouble() <= mutationProbability) {
            return patch;
        }
        return mutateOnce(patch);
    }

    public Patch mutateOnce(Patch patch) {
        System.out.println("Mutating patch: " + patch);
        EditOp op = singleOp(patch);

        // Try a few times to produce a valid mutant; otherwise fall back to originl.
        for (int attempts = 0; attempts < 10; attempts++) {
            int choice = rng.nextInt(3); // 0=changeTarget, 1=changeDonor, 2=flipType
            Patch mutant = switch (choice) {
                case 0 -> changeTarget(op);
                case 1 -> changeDonor(op);
                default -> flipType(op);
            };
            if (mutant != null) {
                System.out.println("Produced mutant: " + mutant);
                return mutant;
            }
        }
        System.out.println("Mutation failed after several attempts, returning original patch.");
        return patch;
    }

    private Patch changeTarget(EditOp op) {
        StatementId newTarget = sampler.getTarget();

        if (op instanceof DeleteOp) {
            return new Patch(List.of(new DeleteOp(newTarget)));
        }
        if (op instanceof ReplaceOp) {
            StatementId donor = pickDonorForTarget(newTarget);
            if (donor == null || donor.equals(newTarget)) return null;
            return new Patch(List.of(new ReplaceOp(newTarget, donor)));
        }
        return null;
    }

    private Patch changeDonor(EditOp op) {
        if (op instanceof ReplaceOp rep) {
            StatementId target = rep.target();
            StatementId newDonor = pickDonorForTarget(target);
            if (newDonor == null) return null;
            // avoid self replace if possible
            if (newDonor.equals(target)) return null;
            return new Patch(List.of(new ReplaceOp(target, newDonor)));
        }
        // If it's delete, changeDonor doesn't apply, treat it as noop
        return null;
    }

    private Patch flipType(EditOp op) {
        if (op instanceof DeleteOp del) {
            StatementId target = del.target();
            StatementId donor = pickDonorForTarget(target);
            if (donor == null) return null;
            if (donor.equals(target)) return null;
            return new Patch(List.of(new ReplaceOp(target, donor)));
        }
        if (op instanceof ReplaceOp rep) {
            return new Patch(List.of(new DeleteOp(rep.target())));
        }
        return null;
    }


    private StatementId pickDonorForTarget(StatementId target) {
        var all = collector.allStatementIds();
        if (all.isEmpty()) return null;

        if (!sameTypeDonorOnly) {
            return all.get(rng.nextInt(all.size()));
        }

        // same type donor selection (mention in report - improves compile rate)
        Class<?> targetType = collector.getStatement(target).getClass();
        // try a few random donors first (cuz cheap)
        for (int i = 0; i < 30; i++) {
            StatementId candidate = all.get(rng.nextInt(all.size()));
            if (collector.getStatement(candidate).getClass().equals(targetType)) {
                return candidate;
            }
        }

        for (StatementId candidate : all) {
            if (collector.getStatement(candidate).getClass().equals(targetType)) {
                return candidate;
            }
        }
        return null;
    }

    private static EditOp singleOp(Patch patch) {
        List<EditOp> edits = patch.edits();
        if (edits == null || edits.size() != 1) {
            throw new IllegalArgumentException("Expected single edit patch, got: " + (edits == null ? 0 : edits.size()));
        }
        return edits.get(0);
    }
}
