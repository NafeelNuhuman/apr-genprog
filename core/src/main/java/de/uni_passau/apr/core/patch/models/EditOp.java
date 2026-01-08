package de.uni_passau.apr.core.patch.models;

public sealed interface EditOp permits DeleteOp, ReplaceOp { }
