package ru.homyakin.seeker.utils;

import java.util.List;

public final class PageUtils {
    public static final int ITEM_LIST_PAGE_SIZE = 10;

    private PageUtils() {
    }

    public static int totalPages(int itemCount) {
        if (itemCount <= 0) {
            return 1;
        }
        return (itemCount + ITEM_LIST_PAGE_SIZE - 1) / ITEM_LIST_PAGE_SIZE;
    }

    public static int clampPage(int page, int totalPages) {
        return Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0));
    }

    public static <T> List<T> pageSlice(List<T> items, int page) {
        if (items.isEmpty()) {
            return List.of();
        }
        final var totalPages = totalPages(items.size());
        final var safePage = clampPage(page, totalPages);
        final var from = safePage * ITEM_LIST_PAGE_SIZE;
        final var to = Math.min(from + ITEM_LIST_PAGE_SIZE, items.size());
        return items.subList(from, to);
    }
}
