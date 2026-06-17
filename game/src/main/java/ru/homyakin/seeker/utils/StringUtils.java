package ru.homyakin.seeker.utils;

import java.util.Optional;

public class StringUtils {
    public static Optional<Integer> findLastOrNeededEntrance(String text, String entrance, int position) {
        int tempIndex = -1;
        int returnIndex = -1;
        for (int i = 0; i < position; i++) {
            tempIndex = text.indexOf(entrance, tempIndex + 1);
            if (tempIndex == -1) {
                break;
            }
            returnIndex = tempIndex;
        }
        if (returnIndex == -1) {
            return Optional.empty();
        }
        return Optional.of(returnIndex);
    }
}
