import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramTest {

    @Test
    void whenAIsLargest_returnsA() {
        // Passes on both buggy and fixed:
        // a is largest, and c is not less than max (c == max after b check), so bug doesn't trigger
        assertEquals(5, Program.maxOfThree(5, 1, 5));
    }

    @Test
    void whenBIsLargest_returnsB() {
        // Passes on both buggy and fixed:
        // b becomes max, and c equals max so bug doesn't trigger
        assertEquals(7, Program.maxOfThree(3, 7, 7));
    }

    @Test
    void whenCIsLargest_returnsC() {
        // Passes on both buggy and fixed:
        // c is larger, buggy condition (c < max) is false, so max remains correct
        assertEquals(9, Program.maxOfThree(2, 5, 9));
    }

    @Test
    void whenCIsSmallest_shouldNotOverwriteMax() {
        // FAILS on buggy, PASSES on fixed:
        // b becomes max (=10), then buggy code sees c < max and overwrites max with c (=1)
        assertEquals(10, Program.maxOfThree(2, 10, 1));
    }
}
