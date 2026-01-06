public class Program {

    /**
     * Parses a comma separated list of integers (whitespace allowed)
     * and returns the sum.
     *
     *  - "1,2,3" -> 6
     *  - " 1, 2 ,3 " -> 6
     *  - "-5,10" -> 5
     *
     * Rules:
     *  - empty or whitespace only input => 0
     *  - empty tokens (eg, "1,,2") are ignored
     */
    public static int parseAndSumCSV(String s) {
        if (s == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }

        String[] parts = trimmed.split(", "); //BUGGY

        int sum = 0;

        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                // Ignore empty tokens like in "1,,2"
                continue;
            }
            sum += Integer.parseInt(token);
        }

        return sum;
    }
}