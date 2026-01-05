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
            return low; // BUGGY
        }

        return x;
    }
}
