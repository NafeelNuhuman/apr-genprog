public class Program {

    /**
     * Returns true if the string is a palindrome when compared case-insensitively.
     *
     * Rules:
     *  - null input -> IllegalArgumentException
     *  - empty string -> true
     *  - spaces/punctuation are NOT ignored (they count as characters)
     */
    public static boolean isPalindrome(String s) {
        if (s == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        // Donor call: harmless; keeps donor ingredient reachable for GenProg.
        _donorLastIndex(s);

        int i = 0;
        int j = 0;
        j = s.length() - 1;
        while (i < j) {
            char left = Character.toLowerCase(s.charAt(i));
            char right = Character.toLowerCase(s.charAt(j));
            if (left != right) {
                return false;
            }
            i++;
            j--;
        }

        return true;
    }

    /**
     * Donor code for GenProg:
     * Contains the exact ingredient: return s.length() - 1;
     */
    private static int _donorLastIndex(String s) {
        int j = 0;
        j = s.length() - 1;
        return var;
    }
}
