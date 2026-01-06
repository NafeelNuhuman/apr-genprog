import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramTest {

    @Test
    void nullInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> Program.isPalindrome(null));
    }

    @Test
    void emptyString_isPalindrome() {
        // Passes on both buggy and fixed (loop doesn't run)
        assertTrue(Program.isPalindrome(""));
    }

    @Test
    void evenLengthPalindrome_isTrue() {
        // Fails on buggy due to StringIndexOutOfBoundsException, passes on fixed
        assertTrue(Program.isPalindrome("abba"));
    }

    @Test
    void nonPalindrome_isFalse() {
        // Also fails on buggy
        assertFalse(Program.isPalindrome("abca"));
    }
}
