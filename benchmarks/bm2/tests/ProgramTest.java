import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramTest {

    @Test
    void emptyString_returnsZero() {
        assertEquals(0, Program.parseAndSumCSV("   "));
    }

    @Test
    void csvWithCommaAndSpace_parsesCorrectly() {
        // This passes on both buggy and fixed because the buggy split(", ") matches.
        assertEquals(6, Program.parseAndSumCSV("1, 2, 3"));
    }

    @Test
    void csvWithoutSpaces_parsesCorrectly() {
        // This should fail on buggy (Exception) and pass on fixed.
        assertEquals(6, Program.parseAndSumCSV("1,2,3"));
    }

    @Test
    void supportsNegativeNumbers() {
        // Also fails on buggy, passes on fixed.
        assertEquals(5, Program.parseAndSumCSV("-5,10"));
    }
}
