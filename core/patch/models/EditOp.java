package de.uni_passau.apr.core.patch;

public sealed interface EditOp permits DeleteOp, ReplaceOp { }
