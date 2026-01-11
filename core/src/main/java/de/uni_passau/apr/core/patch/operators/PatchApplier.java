package de.uni_passau.apr.core.patch.operators;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.Range;

import de.uni_passau.apr.core.patch.models.*;

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
     * Applies a patch to a Java source file and returns the modified source as a String.
     */
    public static String apply(Path javaFile, Patch patch) throws IOException {
        Objects.requireNonNull(javaFile, "javaFile");
        Objects.requireNonNull(patch, "patch");

        CompilationUnit cu = parse(javaFile);
        LexicalPreservingPrinter.setup(cu);

        // Apply bottom up so earlier edits don't shift ranges for later ones
        List<EditOp> edits = patch.edits().stream()
                .sorted(Comparator.comparingInt(PatchApplier::targetBeginLine).reversed())
                .toList();

        for (EditOp op : edits) {
            if (op instanceof DeleteOp del) {
                Statement target = findStatementById(cu, del.target());
                target.remove();
            } else if (op instanceof ReplaceOp rep) {
                if (rep.target().equals(rep.donor())) {
                    throw new IllegalArgumentException("ReplaceOp donor equals target: " + rep.target());
                }
                Statement target = findStatementById(cu, rep.target());
                Statement donor = findStatementById(cu, rep.donor());

                // safety - (already enforcing same statements but tbs)
                if (!target.getClass().equals(donor.getClass())) continue;

                Statement donorClone = donor.clone();
                target.replace(donorClone);
            } else {
                throw new IllegalArgumentException("Unknown EditOp type: " + op.getClass());
            }
        }

        return LexicalPreservingPrinter.print(cu);
    }

    /**
     * Applies a patch and writes the modified source to outputFile (creates parent dirs if needed).
     */
    public static void applyToFile(Path inputFile, Path outputFile, Patch patch) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        String modified = apply(inputFile, patch);

        Path parent = outputFile.getParent();
        if (parent != null) Files.createDirectories(parent);

        Files.writeString(outputFile, modified, StandardCharsets.UTF_8);
    }

    // ------------------- helpers ----------------

    private static int targetBeginLine(EditOp op) {
        StatementId id = (op instanceof DeleteOp d) ? d.target() : ((ReplaceOp) op).target();
        return id.beginLine();
    }

    private static CompilationUnit parse(Path javaFile) throws IOException {
        ParserConfiguration cfg = new ParserConfiguration();
        JavaParser parser = new JavaParser(cfg);

        String src = Files.readString(javaFile, StandardCharsets.UTF_8);
        ParseResult<CompilationUnit> result = parser.parse(src);

        return result.getResult()
                .orElseThrow(() -> new IllegalArgumentException("Parse failed: " + result.getProblems()));
    }

    /**
     * Finds a Statement node whose Range exactly matches the StatementId.
     * Throws if not found.
     */
    private static Statement findStatementById(CompilationUnit cu, StatementId id) {
        for (Statement stmt : cu.findAll(Statement.class)) {
            if (stmt.getRange().isEmpty()) continue;
            Range r = stmt.getRange().get();
            StatementId stmtId = new StatementId(
                    r.begin.line, r.begin.column,
                    r.end.line, r.end.column
            );
            if (stmtId.equals(id)) {
                return stmt;
            }
        }
        throw new IllegalArgumentException("Statement not found for id: " + id);
    }
}
