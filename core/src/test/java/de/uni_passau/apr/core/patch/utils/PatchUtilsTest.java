package de.uni_passau.apr.core.patch.utils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import de.uni_passau.apr.core.patch.models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatchUtilsTest {

    @TempDir
    Path tmp;

    @Test
    void parse_validJava_returnsCompilationUnit() throws Exception {
        Path f = writeJava("A.java", validProgram());
        CompilationUnit cu = PatchUtils.parse(f);
        assertNotNull(cu);
        assertTrue(cu.toString().contains("class A"));
    }

    @Test
    void parse_nonExistingFile_throwsIOException() {
        Path missing = tmp.resolve("missing.java");
        assertThrows(IOException.class, () -> PatchUtils.parse(missing));
    }

    @Test
    void findStatementById_existingStatement_returnsStatement() throws Exception {
        Path f = writeJava("A.java", validProgram());
        CompilationUnit cu = PatchUtils.parse(f);

        StatementId id = idOfStatementContaining(cu, "int b = 2;");
        Statement stmt = PatchUtils.findStatementById(cu, id);

        assertNotNull(stmt);
        assertTrue(PatchUtils.oneLine(stmt.toString()).contains("int b = 2;"));
    }

    @Test
    void findStatementById_missingStatement_throwsIllegalArgumentException() throws Exception {
        Path f = writeJava("A.java", validProgram());
        CompilationUnit cu = PatchUtils.parse(f);

        StatementId bogus = new StatementId(999, 1, 999, 2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PatchUtils.findStatementById(cu, bogus));
        assertTrue(ex.getMessage().contains("Statement not found"));
    }

    @Test
    void targetBeginLine_deleteOp_returnsTargetBeginLine() {
        StatementId id = new StatementId(10, 2, 10, 20);
        EditOp op = new DeleteOp(id);
        assertEquals(10, PatchUtils.targetBeginLine(op));
    }

    @Test
    void targetBeginLine_replaceOp_returnsTargetBeginLine() {
        StatementId target = new StatementId(7, 1, 7, 10);
        StatementId donor  = new StatementId(20, 1, 20, 10);
        EditOp op = new ReplaceOp(target, donor);
        assertEquals(7, PatchUtils.targetBeginLine(op));
    }

    @Test
    void formatId_formatsExactly() {
        StatementId id = new StatementId(3, 4, 5, 6);
        assertEquals("3:4-5:6", PatchUtils.formatId(id));
    }

    @Test
    void oneLine_replacesNewlinesAndCarriageReturnsAndTrims() {
        String s = "  hello\r\nworld\n  ";
        assertEquals("hello  world", PatchUtils.oneLine(s));
        // Explanation of expected:
        // "\r" -> space, "\n" -> space, then trim => "hello  world" (two spaces between due to \r + \n)
    }

    // ---------------- helpers ----------------

    private Path writeJava(String name, String content) throws IOException {
        Path p = tmp.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }

    private String validProgram() {
        return """
                package t;
                public class A {
                  public int f(int x) {
                    int a = 1;
                    int b = 2;
                    return a + b + x;
                  }
                }
                """;
    }

    private StatementId idOfStatementContaining(CompilationUnit cu, String needle) {
        for (Statement stmt : cu.findAll(Statement.class)) {
            if (stmt.getRange().isEmpty()) continue;

            String oneLine = PatchUtils.oneLine(stmt.toString());
            if (oneLine.contains(needle)) {
                var r = stmt.getRange().get();
                return new StatementId(r.begin.line, r.begin.column, r.end.line, r.end.column);
            }
        }
        fail("Could not find statement containing: " + needle);
        return null; // unreachable
    }
}
