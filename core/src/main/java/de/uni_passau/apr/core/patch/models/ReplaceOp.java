package de.uni_passau.apr.core.patch.models;

public record ReplaceOp(StatementId target, StatementId donor) implements EditOp { }
