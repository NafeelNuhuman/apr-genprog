import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;

import java.nio.file.Path;
import java.util.*;

/** Get all statement nodes with their statementIds in a file **/
public final class StatementCollector {

    private final CompilationUnit cu;
    private final Map<StatementId, Statement> byId;
    private final List<StatementId> allIds;

    private StatementCollector(CompilationUnit cu,
                               Map<StatementId, Statement> byId,
                               List<StatementId> allIds) {
        this.cu = cu;
        this.byId = byId;
        this.allIds = allIds;
    }

    public static StatementIndex fromFile(Path javaFile) throws Exception {
        ParserConfiguration cfg = new ParserConfiguration();
        JavaParser parser = new JavaParser(cfg);

        ParseResult<CompilationUnit> result = parser.parse(javaFile);
        CompilationUnit cu = result.getResult()
                .orElseThrow(() -> new IllegalArgumentException("Parse failed: " + result.getProblems()));

        Map<StatementId, Statement> map = new LinkedHashMap<>();
        cu.findAll(Statement.class).forEach(stmt -> {
            stmt.getRange().ifPresent(r -> {
                StatementId id = new StatementId(
                        r.begin.line, r.begin.column,
                        r.end.line, r.end.column
                );
                map.put(id, stmt);
            });
        });

        return new StatementCollector(cu, map, new ArrayList<>(map.keySet()));
    }


    public CompilationUnit cu() { return cu; }
    public List<StatementId> allStatementIds() { return Collections.unmodifiableList(allIds); }

    public Statement getStmt(StatementId id) {
        Statement s = byId.get(id);
        if (s == null) throw new IllegalArgumentException("Unknown statement id: " + id);
        return s;
    }
}
