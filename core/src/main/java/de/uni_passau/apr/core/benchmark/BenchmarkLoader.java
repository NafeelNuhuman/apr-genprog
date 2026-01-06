package de.uni_passau.apr.core.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BenchmarkLoader {

    private final Path benchmarkRoot;

    public BenchmarkLoader(Path benchmarkRoot) {
        if (benchmarkRoot == null) {
            throw new IllegalArgumentException("Benchmark root cannot be null");
        }
        if (!Files.exists(benchmarkRoot)) {
            throw new IllegalArgumentException("Benchmark root does not exist: " + benchmarkRoot);
        }
        if (!Files.isDirectory(benchmarkRoot)) {
            throw new IllegalArgumentException("Benchmark root must be a directory: " + benchmarkRoot);
        }
        this.benchmarkRoot = benchmarkRoot.toAbsolutePath().normalize();
    }

    private Path findSingleFileBasedOnExt(Path dir, String label, String fileExtension) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException(label + " directory does not exist: " + dir);
        }
        try (var stream = Files.list(dir)) {
            var files = stream
                    .filter(path -> path.getFileName().toString().endsWith(fileExtension) && Files.isRegularFile(path))
                    .toList();
            if (files.size() != 1) {
                throw new IllegalArgumentException("Expected exactly one " + label + " file with extension " + fileExtension + " in directory: " + dir + ", but found " + files.size());
            } else if (Files.readString(files.get(0), StandardCharsets.UTF_8).isBlank()) {
                throw new IllegalArgumentException(label + " file is empty: " + files.get(0));
            }
            return files.get(0);
        }
    }

    public BenchmarkConfig load(String benchmarkName) throws IOException {
        if (benchmarkName == null || benchmarkName.isEmpty()) {
            throw new IllegalArgumentException("Benchmark name cannot be null or empty");
        }
        Path benchmarkPath = benchmarkRoot.toAbsolutePath().resolve(benchmarkName).normalize();
        if (!benchmarkPath.startsWith(benchmarkRoot)) {
            throw new IllegalArgumentException("Benchmark path traversal detected: " + benchmarkPath);
        }
        if (!Files.exists(benchmarkPath)) {
            throw new IllegalArgumentException("Benchmark does not exist: " + benchmarkPath);
        }
        if (!Files.isDirectory(benchmarkPath)) {
            throw new IllegalArgumentException("Benchmark is not a directory: " + benchmarkPath);
        }

        Path buggyPath = benchmarkPath.resolve("buggy");
        Path buggyFile = findSingleFileBasedOnExt(buggyPath, "buggy", ".java");
        Path fixedPath = benchmarkPath.resolve("fixed");
        Path fixedFile = findSingleFileBasedOnExt(fixedPath, "fixed", ".java");
        Path testSuitePath = benchmarkPath.resolve("tests");
        Path testSuiteFile = findSingleFileBasedOnExt(testSuitePath, "tests", ".java");
        // fault localization file is a json file at benchmarkRoot/BenchmarkName
        Path faultLocFile = benchmarkPath.resolve("faultloc.json");
        if (!Files.exists(faultLocFile) || !Files.isRegularFile(faultLocFile)) {
            throw new IllegalArgumentException("Fault localization file does not exist: " + faultLocFile);
        }

        BenchmarkConfig config = new BenchmarkConfig();
        config.setName(benchmarkName);
        config.setBenchmarkRoot(benchmarkPath);
        config.setBuggyProgramPath(buggyFile);
        config.setBuggyProgram(Files.readString(buggyFile, StandardCharsets.UTF_8));
        config.setFixedProgramPath(fixedFile);
        config.setFixedProgram(Files.readString(fixedFile, StandardCharsets.UTF_8));
        config.setTestSuitePath(testSuiteFile);
        config.setTestSuite(Files.readString(testSuiteFile, StandardCharsets.UTF_8));
        config.setFaultLocFile(faultLocFile);

        return config;
    }
}
