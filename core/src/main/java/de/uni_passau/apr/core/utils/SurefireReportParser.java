package de.uni_passau.apr.core.utils;

import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * To read Maven Surefire XML test report files and summarize the results
 * (how many tests ran, failed, errored, or were skipped)
 * and list the IDs of failed tests.
 */
public class SurefireReportParser {

    /**
     * Parse the Surefire XML reports and summarize the test results.
     *
     * @param reportPath The path to the dir with Surefire XML reports.
     * @return A TestReportSummary obj with the summarized test results.
     * @throws IOException If an I/O error happens while reading the report files.
     */
    public static TestReportSummary parse(Path reportPath) throws IOException {
        TestReportSummary summary = new TestReportSummary();
        if (!Files.exists(reportPath) || !Files.isDirectory(reportPath)) {
            return summary;
        }
        System.out.println("Parsing surefire reports in directory: " + reportPath);
        LinkedHashSet<String> failTestsIds = new LinkedHashSet<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to configure XML parser for surefire report parsing", e);
        }

        List<Path> reportFiles;
        try (var paths = Files.list(reportPath)) {
            reportFiles = paths
                    .filter(p -> p.getFileName().toString().startsWith("TEST-")
                            && p.getFileName().toString().endsWith(".xml")
                            && Files.isRegularFile(p)).toList();
        } catch (IOException e) {
            throw new IOException("Failed to list surefire report files in directory: " + reportPath, e);
        }
        System.out.println("Found " + reportFiles.size() + " surefire report files.");

        try {
            var dBuilder = dbFactory.newDocumentBuilder();
            for (Path p : reportFiles) {
                var doc = dBuilder.parse(p.toFile());
                doc.getDocumentElement().normalize();
                var testSuite = doc.getDocumentElement();
                int tests = getIntAttribute(testSuite, "tests");
                int failures = getIntAttribute(testSuite, "failures");
                int errors = getIntAttribute(testSuite, "errors");
                int skipped = getIntAttribute(testSuite, "skipped");
                summary.setTestsRun(summary.getTestsRun() + tests);
                summary.setFailures(summary.getFailures() + failures);
                summary.setErrors(summary.getErrors() + errors);
                summary.setSkipped(summary.getSkipped() + skipped);
                // collect failed test IDs (classname#name)
                var testCases = testSuite.getElementsByTagName("testcase");
                for (int i = 0; i < testCases.getLength(); i++) {
                    Element testCase = (Element) testCases.item(i);
                    String className = testCase.getAttribute("classname");
                    String testName = testCase.getAttribute("name");
                    boolean isFailed = testCase.getElementsByTagName("failure").getLength() > 0 ||
                            testCase.getElementsByTagName("error").getLength() > 0;
                    if (isFailed) {
                        String testID = className + "#" + testName;
                        failTestsIds.add(testID);
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create XML document builder for surefire report parsing", e);
        } catch (Exception e) {
            throw new IOException("Failed to parse surefire report files in directory: " + reportPath, e);
        }

        summary.setFailedTestIds(failTestsIds.stream().toList());
        return summary;
    }

    /**
     * To parse int attributes from XML elements.
     * returns 0 if the attribute is missing or not a valid int.
     *
     * @param element       The XML element.
     * @param attributeName The name of the attribute to parse.
     * @return The int value of the attribute, or 0 if fails.
     */
    private static int getIntAttribute(Element element, String attributeName) {
        String attrValue = element.getAttribute(attributeName);
        try {
            return Integer.parseInt(attrValue);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
