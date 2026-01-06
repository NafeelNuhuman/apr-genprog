public class Program {

    /**
     * Returns the max of three integers.
     *
     * Rules:
     *  - Works for negative numbers as well.
     */
    public static int maxOfThree(int a, int b, int c) {
        int max = a;
        if (b > max) {
            max = b;
        }
        if (c < max) { //BUGGY
            max = c;
        }
        return max;
    }
}
