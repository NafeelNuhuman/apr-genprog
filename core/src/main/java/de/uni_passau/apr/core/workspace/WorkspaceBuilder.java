package de.uni_passau.apr.core.workspace;

import de.uni_passau.apr.core.benchmark.BenchmarkConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds a fresh temporary Maven workspace
 * for one benchmark and one candidate variant.
 */
public class WorkspaceBuilder {

    private static final String pomContent = """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>de.uni_passau.apr</groupId>
                    <artifactId>workspace</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.14.1</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.4</version>
                                <configuration>
                                    <includes>
                                        <include>**/*Test.java</include>
                                    </includes>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;
    /**
     * Create a fresh temp Maven project for
     * one benchmark + one candidate variant.
     * @param benchmarkConfig   The benchmark configuration.
     * @param candidateVariant  The candidate variant source code.
     * @return The path to the created workspace.
     */
    public Path build(BenchmarkConfig benchmarkConfig, String candidateVariant) throws IOException {
        Path workspacePath = Files.createTempDirectory("apr-" + benchmarkConfig.getName() + "-");

        String testFileName = benchmarkConfig.getTestSuitePath().getFileName().toString();
        String programFileName = benchmarkConfig.getBuggyProgramPath().getFileName().toString();

        // Create pom.xml file
        Files.writeString(workspacePath.resolve("pom.xml"), pomContent);

        // Create candidate variant source file at src/main/java
        Files.createDirectories(workspacePath.resolve("src/main/java"));
        Files.writeString(workspacePath.resolve("src/main/java/" + programFileName), candidateVariant, java.nio.charset.StandardCharsets.UTF_8);

        // Create test suite source file at src/test/java
        Files.createDirectories(workspacePath.resolve("src/test/java"));
        Path dest = workspacePath.resolve("src/test/java").resolve(testFileName);
        // copy, replace if existing
        Files.copy(benchmarkConfig.getTestSuitePath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return workspacePath;
    }

}
