public class Program {

    /**
     * Returns the maximum of three integers.
     *
     * Rules:
     *  - Works for negative numbers as well.
     */
    public static int maxOfThree(int a, int b, int c) {
        int max = a;
        if (b > max) {
            max = b;
        }
        if (c > max) {
            max = c;
        }
        return max;
    }


    private static boolean _donorGreaterThan() {
        int c, max;
        c = 1;
        max = 0;
        return c > max;
    }

}
