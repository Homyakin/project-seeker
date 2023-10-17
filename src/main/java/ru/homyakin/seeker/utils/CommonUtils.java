package ru.homyakin.seeker.utils;

import java.util.Map;

public class CommonUtils {
    public static <T> T ifNullThan(T obj1, T obj2) {
        return obj1 == null ? obj2 : obj1;
    }

    public static <K, V> void putIfKeyPresents(Map<K, V> map, K key, V value) {
        if (key != null) {
            map.put(key, value);
        }
    }
}
