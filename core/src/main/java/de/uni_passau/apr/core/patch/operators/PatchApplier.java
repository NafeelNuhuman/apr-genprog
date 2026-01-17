package de.uni_passau.apr.core.patch.operators;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import de.uni_passau.apr.core.patch.models.*;
import de.uni_passau.apr.core.patch.utils.PatchUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class PatchApplier {

    private PatchApplier() {}

    /**
     * Applies a patch to a Java source file.
     * @return - String modified source
     */
    public static String apply(Path javaFile, Patch patch) throws IOException {
        Objects.requireNonNull(javaFile, "javaFile");
        Objects.requireNonNull(patch, "patch");

        CompilationUnit cu = PatchUtils.parse(javaFile);
        LexicalPreservingPrinter.setup(cu);

        // Apply bottom up so earlier edits don't shift ranges for later ones
        List<EditOp> edits = patch.edits().stream()
                .sorted(Comparator.comparingInt(PatchApplier::targetBeginLine).reversed())
                .toList();

        for (EditOp op : edits) {
            if (op instanceof DeleteOp del) {
                Statement target = PatchUtils.findStatementById(cu, del.target());
                target.remove();
            } else if (op instanceof ReplaceOp rep) {
                if (rep.target().equals(rep.donor())) {
                    throw new IllegalArgumentException("ReplaceOp donor equals target : " + rep.target());
                }
                Statement target = PatchUtils.findStatementById(cu, rep.target());
                Statement donor = PatchUtils.findStatementById(cu, rep.donor());

                if (!target.getClass().equals(donor.getClass())) continue;

                Statement donorClone = donor.clone();
                target.replace(donorClone);
            } else {
                throw new IllegalArgumentException("Unknown EditOp type : " + op.getClass());
            }
        }

        return LexicalPreservingPrinter.print(cu);
    }

    /**
     * Applies a patch and writes the modified source to outputFile
     * (creates parent dirs if needed).
     */
    public static void applyToFile(Path inputFile, Path outputFile, Patch patch) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        String modified = apply(inputFile, patch);

        Path parent = outputFile.getParent();
        if (parent != null) Files.createDirectories(parent);

        Files.writeString(outputFile, modified, StandardCharsets.UTF_8);
    }

    private static int targetBeginLine(EditOp op) {
        StatementId id = (op instanceof DeleteOp d) ? d.target() : ((ReplaceOp) op).target();
        return id.beginLine();
    }

}
