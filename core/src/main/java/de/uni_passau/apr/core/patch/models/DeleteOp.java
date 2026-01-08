package de.uni_passau.apr.core.patch.models;

public record DeleteOp(StatementId target) implements EditOp { }
