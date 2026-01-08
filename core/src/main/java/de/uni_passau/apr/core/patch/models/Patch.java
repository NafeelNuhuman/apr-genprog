package de.uni_passau.apr.core.patch.models;

import java.util.List;

public record Patch(List<EditOp> edits) {
    public Patch {
        if (edits == null || edits.isEmpty()) {
            throw new IllegalArgumentException("Patch must have edits");
        }
        if (edits.size() > 3) {
            throw new IllegalArgumentException("Keep patches small (<=3) for now");
        }
    }
}
