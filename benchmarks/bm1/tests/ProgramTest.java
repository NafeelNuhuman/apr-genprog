import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramTest {

    @Test
    void belowLowerBound_returnsLow() {
        assertEquals(0, Program.clamp(-1, 0, 10));
    }

    @Test
    void insideRange_returnsX() {
        assertEquals(5, Program.clamp(5, 0, 10));
    }

    @Test
    void aboveUpperBound_returnsHigh() {
        // FAIL on buggy and PASS on fixed.
        assertEquals(10, Program.clamp(99, 0, 10));
    }
}
