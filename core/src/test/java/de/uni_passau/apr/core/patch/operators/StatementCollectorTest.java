package de.uni_passau.apr.core.patch.operators;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import de.uni_passau.apr.core.patch.models.StatementId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatementCollectorTest {

    @TempDir
    Path tmp;

    @Test
    void fromFile_validJava_collectsStatementsAndIds() throws Exception {
        Path f = writeJava("A.java", validProgram());

        StatementCollector sc = StatementCollector.fromFile(f);

        assertNotNull(sc);
        CompilationUnit cu = sc.cu();
        assertNotNull(cu);

        List<StatementId> ids = sc.allStatementIds();
        assertNotNull(ids);
        assertFalse(ids.isEmpty(), "Should collect at least some Statement nodes with ranges");

        // Ensure the IDs correspond to retrievable statements
        StatementId someId = ids.get(0);
        Statement stmt = sc.getStatement(someId);
        assertNotNull(stmt);
        assertTrue(stmt.getRange().isPresent(), "Collected statement should have a range");
    }

    @Test
    void allStatementIds_isUnmodifiable() throws Exception {
        Path f = writeJava("A.java", validProgram());
        StatementCollector sc = StatementCollector.fromFile(f);

        List<StatementId> ids = sc.allStatementIds();
        assertThrows(UnsupportedOperationException.class, () -> ids.add(new StatementId(1, 1, 1, 1)));
    }

    @Test
    void getStatement_knownId_returnsStatementContainingExpectedText() throws Exception {
        Path f = writeJava("A.java", validProgram());
        StatementCollector sc = StatementCollector.fromFile(f);

        // Pick the id for the statement containing "int b = 2;"
        StatementId idB = findIdByStatementText(sc, "int b = 2;");
        Statement stmt = sc.getStatement(idB);

        String oneLine = oneLine(stmt.toString());
        assertTrue(oneLine.contains("int b = 2;"), "Should retrieve the exact statement by id");
    }

    @Test
    void getStatement_unknownId_throwsIllegalArgumentException() throws Exception {
        Path f = writeJava("A.java", validProgram());
        StatementCollector sc = StatementCollector.fromFile(f);

        StatementId bogus = new StatementId(999, 1, 999, 2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sc.getStatement(bogus));
        assertTrue(ex.getMessage().contains("Unknown statement id"));
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

    private StatementId findIdByStatementText(StatementCollector sc, String needle) {
        for (StatementId id : sc.allStatementIds()) {
            Statement stmt = sc.getStatement(id);
            String stmtOneLine = oneLine(stmt.toString());
            if (stmtOneLine.contains(needle)) return id;
        }
        fail("Could not find statement containing: " + needle);
        return null; // unreachable
    }

    private String oneLine(String s) {
        return s.replace("\r", " ").replace("\n", " ").trim();
    }
}
