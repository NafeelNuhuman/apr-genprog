package de.uni_passau.apr.core.faultlocalization.json;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;
import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonFaultLocProviderTest {

    @TempDir
    Path tempDir;

    private final JsonFaultLocProvider provider = new JsonFaultLocProvider();

    @Test
    void loadFor_validJson_filtersZeros_andReturnsFaultLocalization() throws Exception {
        // buggy program has 5 lines
        String buggy = String.join("\n",
                "line1",
                "line2",
                "line3",
                "line4",
                "line5"
        ) + "\n";

        Path json = writeJson(
                """
                {
                  "file": "Buggy.java",
                  "statements": [
                    {"line": 2, "weight": 0.0},
                    {"line": 3, "weight": 0.1},
                    {"line": 5, "weight": 1.0}
                  ]
                }
                """
        );

        BenchmarkConfig cfg = new BenchmarkConfig("bm", tempDir, "Buggy.java", "Fixed.java", "ProgramTest.java", json);
        cfg.setBuggyProgram(buggy); // IMPORTANT: provider uses buggyProgram.lines().count()

        FaultLocalization fl = provider.loadFor(cfg);

        assertEquals("Buggy.java", fl.getFile());
        assertNotNull(fl.getStatements());
        // weight=0.0 should be filtered out
        assertEquals(2, fl.getStatements().size());

        assertEquals(3, fl.getStatements().get(0).getLine());
        assertEquals(0.1, fl.getStatements().get(0).getWeight(), 0.0);

        assertEquals(5, fl.getStatements().get(1).getLine());
        assertEquals(1.0, fl.getStatements().get(1).getWeight(), 0.0);
    }

    @Test
    void loadFor_missingRequiredFields_throwsIllegalArgumentException() throws Exception {
        String buggy = "a\nb\nc\n";
        // missing "file"
        Path json1 = writeJson(
                """
                { "statements": [ {"line": 1, "weight": 0.1} ] }
                """
        );
        BenchmarkConfig cfg1 = baseCfg(json1, buggy);
        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(cfg1));

        // missing "statements"
        Path json2 = writeJson(
                """
                { "file": "Buggy.java" }
                """
        );
        BenchmarkConfig cfg2 = baseCfg(json2, buggy);
        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(cfg2));

        // empty statements
        Path json3 = writeJson(
                """
                { "file": "Buggy.java", "statements": [] }
                """
        );
        BenchmarkConfig cfg3 = baseCfg(json3, buggy);
        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(cfg3));
    }

    @Test
    void loadFor_invalidLineNumberOrNegativeWeight_throws() throws Exception {
        String buggy = "a\nb\nc\n";

        // line <= 0
        Path json1 = writeJson(
                """
                { "file": "Buggy.java", "statements": [ {"line": 0, "weight": 0.1} ] }
                """
        );
        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(baseCfg(json1, buggy)));

        // weight < 0 (beyond epsilon)
        Path json2 = writeJson(
                """
                { "file": "Buggy.java", "statements": [ {"line": 1, "weight": -0.5} ] }
                """
        );
        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(baseCfg(json2, buggy)));
    }

    @Test
    void loadFor_lineOutOfRange_throws() throws Exception {
        // 3 lines only
        String buggy = "a\nb\nc\n";

        Path json = writeJson(
                """
                { "file": "Buggy.java", "statements": [ {"line": 4, "weight": 0.1} ] }
                """
        );

        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(baseCfg(json, buggy)));
    }

    @Test
    void loadFor_invalidWeightValue_throws() throws Exception {
        String buggy = "a\nb\nc\nd\n";

        Path json = writeJson(
                """
                { "file": "Buggy.java", "statements": [ {"line": 2, "weight": 0.2} ] }
                """
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> provider.loadFor(baseCfg(json, buggy)));
        assertTrue(ex.getMessage().contains("invalid weight value"));
    }

    @Test
    void loadFor_duplicateLines_throws() throws Exception {
        String buggy = "a\nb\nc\nd\n";

        Path json = writeJson(
                """
                {
                  "file": "Buggy.java",
                  "statements": [
                    {"line": 2, "weight": 0.1},
                    {"line": 2, "weight": 1.0}
                  ]
                }
                """
        );

        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(baseCfg(json, buggy)));
    }

    @Test
    void loadFor_allWeightsZero_afterFiltering_throws() throws Exception {
        String buggy = "a\nb\nc\nd\n";

        Path json = writeJson(
                """
                {
                  "file": "Buggy.java",
                  "statements": [
                    {"line": 1, "weight": 0.0},
                    {"line": 3, "weight": 0.0}
                  ]
                }
                """
        );

        assertThrows(IllegalArgumentException.class, () -> provider.loadFor(baseCfg(json, buggy)));
    }

    @Test
    void loadFor_snapsWeightsWithinEpsilon_toExactValues() throws Exception {
        // 5 lines
        String buggy = "1\n2\n3\n4\n5\n";

        // slightly-off weights that should snap to 0.1 and 1.0
        Path json = writeJson(
                """
                {
                  "file": "Buggy.java",
                  "statements": [
                    {"line": 2, "weight": 0.1000000001},
                    {"line": 4, "weight": 0.9999999999}
                  ]
                }
                """
        );

        BenchmarkConfig cfg = baseCfg(json, buggy);
        FaultLocalization fl = provider.loadFor(cfg);

        assertEquals(2, fl.getStatements().size());
        assertEquals(0.1, fl.getStatements().get(0).getWeight(), 0.0);
        assertEquals(1.0, fl.getStatements().get(1).getWeight(), 0.0);
    }

    @Test
    void loadFor_whenBuggyProgramIsNull_throwsNullPointerException() throws Exception {
        // Current implementation calls benchmarkConfig.getBuggyProgram().lines()
        // so a null buggyProgram triggers NPE. This test documents that behavior.
        Path json = writeJson(
                """
                { "file": "Buggy.java", "statements": [ {"line": 1, "weight": 0.1} ] }
                """
        );

        BenchmarkConfig cfg = new BenchmarkConfig("bm", tempDir, "Buggy.java", "Fixed.java", "ProgramTest.java", json);
        cfg.setBuggyProgram(null);

        assertThrows(NullPointerException.class, () -> provider.loadFor(cfg));
    }

    // ---------------- helpers ----------------

    private BenchmarkConfig baseCfg(Path json, String buggyProgram) {
        BenchmarkConfig cfg = new BenchmarkConfig("bm", tempDir, "Buggy.java", "Fixed.java", "ProgramTest.java", json);
        cfg.setBuggyProgram(buggyProgram);
        return cfg;
    }

    private Path writeJson(String jsonContent) throws Exception {
        Path file = tempDir.resolve("faultloc.json");
        Files.writeString(file, jsonContent, StandardCharsets.UTF_8);
        return file;
    }
}
