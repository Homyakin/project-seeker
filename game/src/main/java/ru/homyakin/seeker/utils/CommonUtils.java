package ru.homyakin.seeker.utils;

import java.util.Map;
import java.util.Optional;

public class CommonUtils {
    public static <K, V> void putIfKeyPresents(Map<K, V> map, K key, V value) {
        if (key != null) {
            map.put(key, value);
        }
    }

    public static Optional<Integer> parseIntOrEmpty(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
