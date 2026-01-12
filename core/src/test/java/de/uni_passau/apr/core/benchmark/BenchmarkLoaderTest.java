package de.uni_passau.apr.core.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void ctor_nullRoot_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkLoader(null));
    }

    @Test
    void ctor_rootDoesNotExist_throws() {
        Path missing = tempDir.resolve("missing-root");
        assertFalse(Files.exists(missing));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkLoader(missing));
    }

    @Test
    void ctor_rootNotDirectory_throws() throws Exception {
        Path file = tempDir.resolve("root.txt");
        Files.writeString(file, "x", StandardCharsets.UTF_8);
        assertTrue(Files.exists(file));
        assertTrue(Files.isRegularFile(file));

        assertThrows(IllegalArgumentException.class, () -> new BenchmarkLoader(file));
    }

    @Test
    void load_nullOrEmptyName_throws() throws Exception {
        BenchmarkLoader loader = new BenchmarkLoader(tempDir);

        assertThrows(IllegalArgumentException.class, () -> loader.load(null));
        assertThrows(IllegalArgumentException.class, () -> loader.load(""));
    }

    @Test
    void load_pathTraversalDetected_throws() throws Exception {
        // Create a proper benchmarkRoot
        BenchmarkLoader loader = new BenchmarkLoader(tempDir);

        // This resolves outside benchmarkRoot after normalize
        assertThrows(IllegalArgumentException.class, () -> loader.load(".."));
        assertThrows(IllegalArgumentException.class, () -> loader.load("../outside"));
    }

    @Test
    void load_benchmarkDoesNotExist_throws() throws Exception {
        BenchmarkLoader loader = new BenchmarkLoader(tempDir);

        assertThrows(IllegalArgumentException.class, () -> loader.load("NoSuchBenchmark"));
    }

    @Test
    void load_benchmarkIsNotDirectory_throws() throws Exception {
        Path benchFile = tempDir.resolve("BenchA");
        Files.writeString(benchFile, "not a directory", StandardCharsets.UTF_8);

        BenchmarkLoader loader = new BenchmarkLoader(tempDir);
        assertThrows(IllegalArgumentException.class, () -> loader.load("BenchA"));
    }

    @Test
    void load_missingRequiredDirsOrFiles_throws() throws Exception {
        Path bench = createBenchmarkSkeleton("Bench1");
        BenchmarkLoader loader = new BenchmarkLoader(tempDir);

        // Missing buggy/fixed/tests folders => findSingleFileBasedOnExt should complain
        assertThrows(IllegalArgumentException.class, () -> loader.load("Bench1"));

        // Create dirs but no files
        Files.createDirectories(bench.resolve("buggy"));
        Files.createDirectories(bench.resolve("fixed"));
        Files.createDirectories(bench.resolve("tests"));
        assertThrows(IllegalArgumentException.class, () -> loader.load("Bench1"));
    }

    @Test
    void load_whenDirHasZeroOrMoreThanOneJavaFile_throws() throws Exception {
        Path bench = createBenchmarkSkeleton("Bench2");
        Path buggyDir = Files.createDirectories(bench.resolve("buggy"));
        Path fixedDir = Files.createDirectories(bench.resolve("fixed"));
        Path testsDir = Files.createDirectories(bench.resolve("tests"));

        // Need faultloc.json to get past that check later when directories are valid
        Files.writeString(bench.resolve("faultloc.json"), "{}", StandardCharsets.UTF_8);

        BenchmarkLoader loader = new BenchmarkLoader(tempDir);

        // zero java in buggy
        writeJavaFile(fixedDir.resolve("Fixed.java"), "class Fixed {}");
        writeJavaFile(testsDir.resolve("ProgramTest.java"), "class ProgramTest {}");
        assertThrows(IllegalArgumentException.class, () -> loader.load("Bench2"));

        // now add TWO java files in buggy
        writeJavaFile(buggyDir.resolve("Buggy1.java"), "class Buggy1 {}");
        writeJavaFile(buggyDir.resolve("Buggy2.java"), "class Buggy2 {}");
        assertThrows(IllegalArgumentException.class, () -> loader.load("Bench2"));
    }

    @Test
    void load_whenJavaFileIsBlank_throws() throws Exception {
        Path bench = createBenchmarkSkeleton("Bench3");
        Path buggyDir = Files.createDirectories(bench.resolve("buggy"));
        Path fixedDir = Files.createDirectories(bench.resolve("fixed"));
        Path testsDir = Files.createDirectories(bench.resolve("tests"));

        Files.writeString(bench.resolve("faultloc.json"), "{}", StandardCharsets.UTF_8);

        // buggy is blank
        Files.writeString(buggyDir.resolve("Buggy.java"), "   \n\t  ", StandardCharsets.UTF_8);
        writeJavaFile(fixedDir.resolve("Fixed.java"), "class Fixed {}");
        writeJavaFile(testsDir.resolve("ProgramTest.java"), "class ProgramTest {}");

        BenchmarkLoader loader = new BenchmarkLoader(tempDir);
        assertThrows(IllegalArgumentException.class, () -> loader.load("Bench3"));
    }

    @Test
    void load_missingFaultLocFile_throws() throws Exception {
        Path bench = createBenchmarkSkeleton("Bench4");
        Path buggyDir = Files.createDirectories(bench.resolve("buggy"));
        Path fixedDir = Files.createDirectories(bench.resolve("fixed"));
        Path testsDir = Files.createDirectories(bench.resolve("tests"));

        writeJavaFile(buggyDir.resolve("Buggy.java"), "class Buggy {}");
        writeJavaFile(fixedDir.resolve("Fixed.java"), "class Fixed {}");
        writeJavaFile(testsDir.resolve("ProgramTest.java"), "class ProgramTest {}");

        BenchmarkLoader loader = new BenchmarkLoader(tempDir);
        assertThrows(IllegalArgumentException.class, () -> loader.load("Bench4"));
    }

    @Test
    void load_success_populatesBenchmarkConfigFields() throws Exception {
        Path bench = createBenchmarkSkeleton("BenchOK");
        Path buggyDir = Files.createDirectories(bench.resolve("buggy"));
        Path fixedDir = Files.createDirectories(bench.resolve("fixed"));
        Path testsDir = Files.createDirectories(bench.resolve("tests"));

        Path buggyFile = buggyDir.resolve("Buggy.java");
        Path fixedFile = fixedDir.resolve("Fixed.java");
        Path testsFile = testsDir.resolve("ProgramTest.java");
        Path faultLoc = bench.resolve("faultloc.json");

        String buggySrc = "public class Buggy { int x = 1; }";
        String fixedSrc = "public class Fixed { int x = 2; }";
        String testSrc = "public class ProgramTest { }";

        writeJavaFile(buggyFile, buggySrc);
        writeJavaFile(fixedFile, fixedSrc);
        writeJavaFile(testsFile, testSrc);
        Files.writeString(faultLoc, "{\"dummy\":true}", StandardCharsets.UTF_8);

        BenchmarkLoader loader = new BenchmarkLoader(tempDir);
        BenchmarkConfig cfg = loader.load("BenchOK");

        assertEquals("BenchOK", cfg.getName());

        // benchmarkRoot must point to the benchmark directory (not the top-level root)
        assertEquals(bench.toAbsolutePath().normalize(), cfg.getBenchmarkRoot());

        assertEquals(buggyFile.toAbsolutePath().normalize(), cfg.getBuggyProgramPath());
        assertEquals(fixedFile.toAbsolutePath().normalize(), cfg.getFixedProgramPath());
        assertEquals(testsFile.toAbsolutePath().normalize(), cfg.getTestSuitePath());
        assertEquals(faultLoc.toAbsolutePath().normalize(), cfg.getFaultLocFilePath());

        assertEquals(buggySrc, cfg.getBuggyProgram());
        assertEquals(fixedSrc, cfg.getFixedProgram());
        assertEquals(testSrc, cfg.getTestSuite());
    }

    @Test
    void listAvailableBenchmarks_listsOnlyDirectories() throws IOException {
        // directories
        Files.createDirectories(tempDir.resolve("B1"));
        Files.createDirectories(tempDir.resolve("B2"));

        // file (must be ignored)
        Files.writeString(tempDir.resolve("not-a-benchmark.txt"), "x", StandardCharsets.UTF_8);

        BenchmarkLoader loader = new BenchmarkLoader(tempDir);
        List<String> names = loader.listAvailableBenchmarks();

        assertTrue(names.contains("B1"));
        assertTrue(names.contains("B2"));
        assertFalse(names.contains("not-a-benchmark.txt"));
    }

    // ---------------- helpers ----------------

    private Path createBenchmarkSkeleton(String name) throws IOException {
        Path bench = tempDir.resolve(name);
        Files.createDirectories(bench);
        return bench;
    }

    private void writeJavaFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
