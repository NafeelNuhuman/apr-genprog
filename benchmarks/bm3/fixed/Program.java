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

        long sum = 0L;
        for (int v : a) {
            sum += v;
        }
        return sum / (double) a.length;
    }
}
