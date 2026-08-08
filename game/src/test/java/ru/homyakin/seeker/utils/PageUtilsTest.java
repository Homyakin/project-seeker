package ru.homyakin.seeker.utils;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PageUtilsTest {
    @Test
    @DisplayName("Empty list has one page and empty slice")
    public void emptyList() {
        Assertions.assertEquals(1, PageUtils.totalPages(0));
        Assertions.assertEquals(List.of(), PageUtils.pageSlice(List.of(), 0));
        Assertions.assertEquals(List.of(), PageUtils.pageSlice(List.of(), 5));
    }

    @Test
    @DisplayName("Exactly one page of items")
    public void singlePage() {
        final var items = IntStream.range(0, 10).boxed().toList();
        Assertions.assertEquals(1, PageUtils.totalPages(items.size()));
        Assertions.assertEquals(items, PageUtils.pageSlice(items, 0));
        Assertions.assertEquals(items, PageUtils.pageSlice(items, 99));
    }

    @Test
    @DisplayName("Slices by 10 items and clamps page")
    public void multiplePages() {
        final var items = IntStream.range(0, 25).boxed().toList();
        Assertions.assertEquals(3, PageUtils.totalPages(items.size()));
        Assertions.assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), PageUtils.pageSlice(items, 0));
        Assertions.assertEquals(List.of(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), PageUtils.pageSlice(items, 1));
        Assertions.assertEquals(List.of(20, 21, 22, 23, 24), PageUtils.pageSlice(items, 2));
        Assertions.assertEquals(List.of(20, 21, 22, 23, 24), PageUtils.pageSlice(items, 100));
        Assertions.assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), PageUtils.pageSlice(items, -1));
    }
}
