package ru.homyakin.seeker.utils;

import java.util.Map;

public class CommonUtils {
    public static <K, V> void putIfKeyPresents(Map<K, V> map, K key, V value) {
        if (key != null) {
            map.put(key, value);
        }
    }
}
