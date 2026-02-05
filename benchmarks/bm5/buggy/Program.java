public class Program {

    /**
     * Returns the max of three integers.
     *
     * Rules:
     *  - Works for negative numbers as well.
     */
    public static int maxOfThree(int a, int b, int c) {
        // Donor call: harmless; keeps donor ingredient reachable for GenProg.
        _donorGreaterThan();

        int max = a;
        if (b > max) {
            max = b;
        }
        if (c < max) { // BUGGY (should be: c > max)
            max = c;
        }
        return max;
    }


    private static boolean _donorGreaterThan(int c, int max) {
        return c > max;
    }

}
