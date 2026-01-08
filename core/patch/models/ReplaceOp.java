package de.uni_passau.apr.core.patch;

public record ReplaceOp(StatementId target, StatementId donor) implements EditOp { }
