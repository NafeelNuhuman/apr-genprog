import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramTest {

    @Test
    void nullArray_throws() {
        assertThrows(IllegalArgumentException.class, () -> Program.mean(null));
    }

    @Test
    void emptyArray_returnsZero() {
        assertEquals(0.0, Program.mean(new int[]{}), 1e-9);
    }

    @Test
    void meanIsWholeNumber_returnsExact() {
        // Passes on both buggy and fixed (6 / 2 = 3)
        assertEquals(3.0, Program.mean(new int[]{2, 4}), 1e-9);
    }

    @Test
    void meanCanBeFractional_returnsFraction() {
        // FAILS on buggy (3 / 2 = 1), passes on fixed (3 / 2.0 = 1.5)
        assertEquals(1.5, Program.mean(new int[]{1, 2}), 1e-9);
    }
}
