package de.uni_passau.apr.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SurefireReportParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parse_returnsEmptySummary_whenDirDoesNotExist() throws IOException {
        Path missing = tempDir.resolve("missing-reports-dir");
        TestReportSummary summary = SurefireReportParser.parse(missing);

        assertNotNull(summary);
        assertEquals(0, summary.getTestsRun());
        assertEquals(0, summary.getFailures());
        assertEquals(0, summary.getErrors());
        assertEquals(0, summary.getSkipped());

        List<String> failed = summary.getFailedTestIds();
        assertTrue(failed == null || failed.isEmpty());
    }

    @Test
    void parse_returnsEmptySummary_whenPathIsFileNotDirectory() throws IOException {
        Path file = tempDir.resolve("not-a-dir");
        Files.writeString(file, "x", StandardCharsets.UTF_8);

        TestReportSummary summary = SurefireReportParser.parse(file);

        assertNotNull(summary);
        assertEquals(0, summary.getTestsRun());
        assertEquals(0, summary.getFailures());
        assertEquals(0, summary.getErrors());
        assertEquals(0, summary.getSkipped());
        assertTrue(summary.getFailedTestIds() == null || summary.getFailedTestIds().isEmpty());
    }

    @Test
    void parse_parsesCounts_andCollectsFailingIds_fromSingleXml() throws IOException {
        Path reports = tempDir.resolve("surefire-reports");
        Files.createDirectories(reports);

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="ProgramTest" tests="3" failures="1" errors="0" skipped="1">
                    <testcase classname="ProgramTest" name="passes"/>
                    <testcase classname="ProgramTest" name="fails">
                        <failure message="boom">stack</failure>
                    </testcase>
                    <testcase classname="ProgramTest" name="skipped">
                        <skipped/>
                    </testcase>
                </testsuite>
                """;

        Files.writeString(reports.resolve("TEST-ProgramTest.xml"), xml, StandardCharsets.UTF_8);

        TestReportSummary summary = SurefireReportParser.parse(reports);

        assertEquals(3, summary.getTestsRun());
        assertEquals(1, summary.getFailures());
        assertEquals(0, summary.getErrors());
        assertEquals(1, summary.getSkipped());

        assertEquals(List.of("ProgramTest#fails"), summary.getFailedTestIds());
    }

    @Test
    void parse_collectsErrorTestcasesToo() throws IOException {
        Path reports = tempDir.resolve("surefire-reports");
        Files.createDirectories(reports);

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="ErrSuite" tests="2" failures="0" errors="1" skipped="0">
                    <testcase classname="ErrSuite" name="ok"/>
                    <testcase classname="ErrSuite" name="boom">
                        <error message="err">stack</error>
                    </testcase>
                </testsuite>
                """;

        Files.writeString(reports.resolve("TEST-ErrSuite.xml"), xml, StandardCharsets.UTF_8);

        TestReportSummary summary = SurefireReportParser.parse(reports);

        assertEquals(2, summary.getTestsRun());
        assertEquals(0, summary.getFailures());
        assertEquals(1, summary.getErrors());
        assertEquals(0, summary.getSkipped());
        assertEquals(List.of("ErrSuite#boom"), summary.getFailedTestIds());
    }

    @Test
    void parse_sumsAcrossMultipleXmlFiles_andDeduplicatesFailingIds_inInsertionOrder() throws IOException {
        Path reports = tempDir.resolve("surefire-reports");
        Files.createDirectories(reports);

        String xml1 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="A" tests="2" failures="1" errors="0" skipped="0">
                    <testcase classname="A" name="t1">
                        <failure>fail</failure>
                    </testcase>
                    <testcase classname="A" name="t2"/>
                </testsuite>
                """;

        String xml2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="B" tests="2" failures="0" errors="1" skipped="0">
                    <testcase classname="B" name="t3">
                        <error>err</error>
                    </testcase>
                    <!-- same failing test id as in xml1; should be deduped -->
                    <testcase classname="A" name="t1">
                        <failure>fail again</failure>
                    </testcase>
                </testsuite>
                """;

        Files.writeString(reports.resolve("TEST-A.xml"), xml1, StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("TEST-B.xml"), xml2, StandardCharsets.UTF_8);

        TestReportSummary summary = SurefireReportParser.parse(reports);

        assertEquals(4, summary.getTestsRun());
        assertEquals(1, summary.getFailures());
        assertEquals(1, summary.getErrors());
        assertEquals(0, summary.getSkipped());

        assertEquals(List.of("A#t1", "B#t3"), summary.getFailedTestIds());
    }

    @Test
    void parse_ignoresFilesThatDoNotMatchTestXmlNamingConvention() throws IOException {
        Path reports = tempDir.resolve("surefire-reports");
        Files.createDirectories(reports);

        Files.writeString(reports.resolve("random.txt"), "nope", StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("TEST-not-xml.txt"), "nope", StandardCharsets.UTF_8);

        TestReportSummary summary = SurefireReportParser.parse(reports);

        assertEquals(0, summary.getTestsRun());
        assertEquals(0, summary.getFailures());
        assertEquals(0, summary.getErrors());
        assertEquals(0, summary.getSkipped());
        assertTrue(summary.getFailedTestIds() == null || summary.getFailedTestIds().isEmpty());
    }

//    @Test
//    void parse_handlesMalformedXml_gracefully_andStillParsesOtherFiles() throws IOException {
//        Path reports = tempDir.resolve("surefire-reports");
//        Files.createDirectories(reports);
//
//        // Malformed XML
//        Files.writeString(reports.resolve("TEST-Bad.xml"), "<testsuite", StandardCharsets.UTF_8);
//
//        // Valid XML
//        String goodXml = """
//                <?xml version="1.0" encoding="UTF-8"?>
//                <testsuite name="Good" tests="1" failures="1" errors="0" skipped="0">
//                    <testcase classname="Good" name="fails">
//                        <failure>fail</failure>
//                    </testcase>
//                </testsuite>
//                """;
//        Files.writeString(reports.resolve("TEST-Good.xml"), goodXml, StandardCharsets.UTF_8);
//
//        TestReportSummary summary = SurefireReportParser.parse(reports);
//
//        assertEquals(1, summary.getTestsRun());
//        assertEquals(1, summary.getFailures());
//        assertEquals(0, summary.getErrors());
//        assertEquals(0, summary.getSkipped());
//        assertEquals(List.of("Good#fails"), summary.getFailedTestIds());
//    }
}
