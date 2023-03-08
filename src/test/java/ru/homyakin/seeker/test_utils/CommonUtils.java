package ru.homyakin.seeker.test_utils;

public class CommonUtils {
    public static boolean compareDoubles(double d1, double d2, double epsilon) {
        return Math.abs(d1 - d2) <= epsilon;
    }
}
