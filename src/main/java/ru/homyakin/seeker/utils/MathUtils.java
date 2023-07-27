package ru.homyakin.seeker.utils;

public class MathUtils {
    public static int doubleToIntWithMinMaxValues(double d) {
        if (d > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (d < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else {
            return (int) d;
        }
    }

    public static double log(double base, double value) {
        return Math.log(value) / Math.log(base);
    }
}
