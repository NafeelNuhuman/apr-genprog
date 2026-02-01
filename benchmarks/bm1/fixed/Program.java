public class Program {

    /**
     * Clamp x into the inclusive range [low, high].
     *
     * Rules:
     *  - if x < low -> low
     *  - if x > high -> high
     *  - otherwise -> x
     */
    public static int clamp(int x, int low, int high) {
        if (low > high) {
            throw new IllegalArgumentException("low must be <= high");
        }

        if (x < low) {
            return low;
        }

        if (x > high) {
            return high; // FIXED: must return high when x is greater
        }

        return x;
    }

    /**
     * Donor code for GenProg:
     * Contains the exact statement "return high;" so the repair operator can copy it.
     */
    private static int _donorReturnHigh(int high) {
        return high;
    }
}
