package de.uni_passau.apr.core.testrunner;

import java.nio.file.Path;

public interface TestRunner {

    public TestResult runTests(Path workspaceDir);
}
