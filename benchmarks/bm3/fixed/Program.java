public class Program {

    /**
     * Returns the mean of the given int array as a double
     *
     * Rules:
     *  - null input -> IllegalArgumentException
     *  - empty array -> 0.0
     */
    public static double mean(int[] a) {
        if (a == null) {
            throw new IllegalArgumentException("array must not be null");
        }
        if (a.length == 0) {
            return 0.0;
        }

        // Donor call: harmless; keeps donor code reachable for GenProg.
        _donorMeanLike(a);

        long sum = 0L;
        for (int v : a) {
            sum += v;
        }
        return sum / (double) a.length;
    }

    /**
     * Donor code for GenProg:
     * Contains the exact ingredient: return sum / (double) a.length;
     */
    private static double _donorMeanLike(int[] a) {
        long sum = 0L;
        for (int v : a) {
            sum += v;
        }
        return sum / (double) a.length;
    }
}
