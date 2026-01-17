package de.uni_passau.apr.core.patch.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;

import de.uni_passau.apr.core.patch.models.DeleteOp;
import de.uni_passau.apr.core.patch.models.EditOp;
import de.uni_passau.apr.core.patch.models.ReplaceOp;
import de.uni_passau.apr.core.patch.models.StatementId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PatchUtils {

    private PatchUtils() {}

    /** Parse a Java file into a CompilationUnit
     * @exception IOException - (throws on parse failure). */
    public static CompilationUnit parse(Path javaFile) throws IOException {
        ParserConfiguration cfg = new ParserConfiguration();
        JavaParser parser = new JavaParser(cfg);

        String src = Files.readString(javaFile, StandardCharsets.UTF_8);
        ParseResult<CompilationUnit> result = parser.parse(src);

        return result.getResult()
                .orElseThrow(() -> new IllegalArgumentException("Parse failed : " + result.getProblems()));
    }

    /**
     * Find a statement node whose Range exactly match the StatementId.
     */
    public static Statement findStatementById(CompilationUnit cu, StatementId id) {
        for (Statement stmt : cu.findAll(Statement.class)) {
            if (stmt.getRange().isEmpty()) continue;

            Range r = stmt.getRange().get();
            StatementId stmtId = new StatementId(
                    r.begin.line, r.begin.column,
                    r.end.line, r.end.column
            );

            if (stmtId.equals(id)) return stmt;
        }
        throw new IllegalArgumentException("Statement not found for id : " + id);
    }


    public static int targetBeginLine(EditOp op) {
        StatementId id = (op instanceof DeleteOp d) ? d.target() : ((ReplaceOp) op).target();
        return id.beginLine();
    }

    public static String formatId(StatementId id) {
        return String.format("%d:%d-%d:%d",
                id.beginLine(), id.beginCol(),
                id.endLine(), id.endCol());
    }

    public static String oneLine(String s) {
        return s.replace("\r", " ").replace("\n", " ").trim();
    }

}
