package de.uni_passau.apr.core.patch.operators;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;

import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.patch.utils.PatchUtils;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;


/**
 * Produces a human readable description of a Patch with
 * -edit type
 * -target location
 * -before/after statement snippets
 * Note : meant for CLI output + report.
 */
public final class PatchPrinter {

    private PatchPrinter() { }

    /**
     * @param buggyFile original buggy Java file
     * @param patch patch to print
     */
    public static String print(Path buggyFile, Patch patch) throws Exception {
        Objects.requireNonNull(buggyFile, "buggyFile");
        Objects.requireNonNull(patch, "patch");

        CompilationUnit cu = PatchUtils.parse(buggyFile);

        StringBuilder sb = new StringBuilder();
        sb.append("Patch (").append(patch.edits().size()).append(" edit(s))\n");

        var edits = patch.edits().stream()
                .sorted(Comparator.comparingInt(PatchUtils::targetBeginLine))
                .toList();

        int i = 1;
        for (EditOp op : edits) {
            sb.append("\n");
            sb.append("Edit ").append(i++).append(":\n");
            sb.append(printEdit(cu, op));
        }

        return sb.toString();
    }

    /**
     * Print a single edit operation with before/after snippets.
     */
    private static String printEdit(CompilationUnit cu, EditOp op) {
        if (op instanceof DeleteOp del) {
            Statement targetStmt = PatchUtils.findStatementById(cu, del.target());

            return new StringBuilder()
                    .append("  Type   : DELETE\n")
                    .append("  Target : ").append(PatchUtils.formatId(del.target())).append("\n")
                    .append("  Before : ").append(PatchUtils.oneLine(targetStmt.toString())).append("\n")
                    .toString();
        }

        if (op instanceof ReplaceOp rep) {
            Statement targetStmt = PatchUtils.findStatementById(cu, rep.target());
            Statement donorStmt = PatchUtils.findStatementById(cu, rep.donor());

            return new StringBuilder()
                    .append("  Type   : REPLACE\n")
                    .append("  Target : ").append(PatchUtils.formatId(rep.target())).append("\n")
                    .append("  Donor  : ").append(PatchUtils.formatId(rep.donor())).append("\n")
                    .append("  Before : ").append(PatchUtils.oneLine(targetStmt.toString())).append("\n")
                    .append("  After  : ").append(PatchUtils.oneLine(donorStmt.toString())).append("\n")
                    .toString();
        }

        return "  Type   : UNKNOWN (" + op.getClass().getName() + ")\n";
    }

}
