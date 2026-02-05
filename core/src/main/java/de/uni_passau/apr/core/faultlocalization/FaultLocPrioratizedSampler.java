package de.uni_passau.apr.core.faultlocalization;

import de.uni_passau.apr.core.patch.operators.StatementCollector;
import de.uni_passau.apr.core.patch.models.StatementId;

import java.util.*;

public final class FaultLocPrioratizedSampler {

    private final StatementCollector collector;
    private final Random rand;

    private final List<StatementId> ids = new ArrayList<>();
    private final List<Double> cumulative = new ArrayList<>();
    private double total = 0.0;

    private final NavigableMap<Integer, List<StatementId>> byBeginLine = new TreeMap<>();

    public FaultLocPrioratizedSampler(FaultLocalization fl,
                                      StatementCollector collector,
                                      Random rand) {
        this.collector = Objects.requireNonNull(collector);
        this.rand = Objects.requireNonNull(rand);

        // index for nearest-next/prev lookup
        for (StatementId id : collector.allStatementIds()) {
            byBeginLine.computeIfAbsent(id.beginLine(), k -> new ArrayList<>()).add(id);
        }

        buildCandidates(fl);
    }

    private void buildCandidates(FaultLocalization fl) {
        if (fl.getStatements() == null || fl.getStatements().isEmpty()) {
            throw new IllegalStateException("FaultLocalization has no locations: " + fl.getFile());
        }

        // accumulate weights per StatementId (in case multiple lines map to same statement)
        Map<StatementId, Double> weightByStmt = new LinkedHashMap<>();

        for (WeightedLocation wl : fl.getStatements()) {
            int line = wl.getLine();
            double w = wl.getWeight();

            if (w <= 0.0) continue; // 0s are not covered by tests
            StatementId stmt = mapLineToStatement(line);
            weightByStmt.merge(stmt, w, Double::sum);
        }

        ids.clear();
        cumulative.clear();
        total = 0.0;

        for (Map.Entry<StatementId, Double> e : weightByStmt.entrySet()) {
            double w = e.getValue();
            if (w <= 0.0) continue;
            total += w;
            ids.add(e.getKey());
            cumulative.add(total);
        }

        if (ids.isEmpty()) {
            throw new IllegalStateException("Fault localization lines didnt map to any statements: " + fl.getFile());
        }
    }

    private StatementId mapLineToStatement(int line) {
        // 1 choose best statement that contains the line
        StatementId best = null;
        int bestSpan = Integer.MAX_VALUE;

        for (StatementId id : collector.allStatementIds()) {
            if (id.beginLine() <= line && line <= id.endLine()) {
                int span = id.endLine() - id.beginLine();
                if (span < bestSpan) { //get the tightest span so smaller area to consider
                    bestSpan = span;
                    best = id;
                }
            }
        }
        if (best != null) return best;

        // 2 next statement after line
        Map.Entry<Integer, List<StatementId>> next = byBeginLine.ceilingEntry(line + 1);
        if (next != null && !next.getValue().isEmpty()) return next.getValue().get(0);

        // 3 previous statement before line
        Map.Entry<Integer, List<StatementId>> prev = byBeginLine.floorEntry(line - 1);
        if (prev != null && !prev.getValue().isEmpty()) {
            List<StatementId> list = prev.getValue();
            return list.get(list.size() - 1);
        }

        throw new IllegalStateException("No statements available to map to faultloc line " + line);
    }

    // returns one statement to edit, chosen randomly but biased by fl weights
    public StatementId getTarget() {
        double r = rand.nextDouble() * total;
        int idx = Collections.binarySearch(cumulative, r);
        if (idx < 0) idx = -idx - 1;
        return ids.get(idx);
    }

}
