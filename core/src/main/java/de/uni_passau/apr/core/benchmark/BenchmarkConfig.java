package de.uni_passau.apr.core.benchmark;

import java.nio.file.Path;

public class BenchmarkConfig {

    private String name;
    private Path benchmarkRoot;
    private Path buggyProgramPath;
    private Path testSuitePath;
    private Path fixedProgramPath;
    private String buggyProgram;
    private String fixedProgram;
    private String testSuite;
    private Path faultLocFile;

    public BenchmarkConfig(String name, Path benchmarkRoot, String buggyProgram, String fixedProgram, String testSuite, Path faultLocFile) {
        this.name = name;
        this.benchmarkRoot = benchmarkRoot;
        this.buggyProgram = buggyProgram;
        this.fixedProgram = fixedProgram;
        this.testSuite = testSuite;
        this.faultLocFile = faultLocFile;
    }

    public BenchmarkConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getBenchmarkRoot() {
        return benchmarkRoot;
    }

    public void setBenchmarkRoot(Path benchmarkRoot) {
        this.benchmarkRoot = benchmarkRoot;
    }

    public Path getBuggyProgramPath() {
        return buggyProgramPath;
    }

    public void setBuggyProgramPath(Path buggyProgramPath) {
        this.buggyProgramPath = buggyProgramPath;
    }

    public Path getFixedProgramPath() {
        return fixedProgramPath;
    }

    public void setFixedProgramPath(Path fixedProgramPath) {
        this.fixedProgramPath = fixedProgramPath;
    }

    public Path getTestSuitePath() {
        return testSuitePath;
    }

    public void setTestSuitePath(Path testSuitePath) {
        this.testSuitePath = testSuitePath;
    }

    public String getBuggyProgram() {
        return buggyProgram;
    }

    public void setBuggyProgram(String buggyProgram) {
        this.buggyProgram = buggyProgram;
    }

    public String getFixedProgram() {
        return fixedProgram;
    }

    public void setFixedProgram(String fixedProgram) {
        this.fixedProgram = fixedProgram;
    }

    public String getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(String testSuite) {
        this.testSuite = testSuite;
    }

    public Path getFaultLocFile() {
        return faultLocFile;
    }

    public void setFaultLocFile(Path faultLocFile) {
        this.faultLocFile = faultLocFile;
    }
}
