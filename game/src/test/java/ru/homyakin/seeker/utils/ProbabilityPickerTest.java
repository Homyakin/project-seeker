package ru.homyakin.seeker.utils;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProbabilityPickerTest {
    @Test
    void Given_MockRandom_When_Pick_Then_ReturnsCorrectItem() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("A", 7);
        map.put("B", 2);
        map.put("C", 1);
        ProbabilityPicker<String> picker = new ProbabilityPicker<>(map);
        // Weights: [A:0-6], [B:7-8], [C:9]
        assertEquals("A", picker.pick(_ -> 0)); // 0 in A
        assertEquals("A", picker.pick(_ -> 6)); // 6 in A
        assertEquals("B", picker.pick(_ -> 7)); // 7 in B
        assertEquals("B", picker.pick(_ -> 8)); // 8 in B
        assertEquals("C", picker.pick(_ -> 9)); // 9 in C
    }

    @Test
    void Given_EmptyMap_When_Construct_Then_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new ProbabilityPicker<>(Collections.emptyMap()));
    }

    @Test
    void Given_NegativeWeight_When_Construct_Then_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", -1);
        assertThrows(IllegalArgumentException.class, () -> new ProbabilityPicker<>(map));
    }

    @Test
    void Given_ZeroTotalWeight_When_Construct_Then_ThrowsException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 0);
        map.put("B", 0);
        assertThrows(IllegalArgumentException.class, () -> new ProbabilityPicker<>(map));
    }
}
