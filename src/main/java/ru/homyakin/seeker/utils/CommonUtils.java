package ru.homyakin.seeker.utils;

public class CommonUtils {
    public static <T> T ifNullThan(T obj1, T obj2) {
        return obj1 == null ? obj2 : obj1;
    }
}
