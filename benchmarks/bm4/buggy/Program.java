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

        int i = 0;
        int j = s.length(); //BUGGY

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
}
