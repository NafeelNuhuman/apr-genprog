package de.uni_passau.apr.core.testrunner;

import java.nio.file.Path;

/**
 * Interface for running tests in a given workspace directory.
 */
public interface TestRunner {

    public TestResult runTests(Path workspaceDir);
}
