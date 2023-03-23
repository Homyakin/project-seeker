package ru.homyakin.seeker.utils;

public class StringUtils {
    public static int findLastOrNeededEntrance(String text, String entrance, int position) {
        int tempIndex = -1;
        int returnIndex = -1;
        for (int i = 0; i < position; i++) {
            tempIndex = text.indexOf(entrance, tempIndex + 1);
            if (tempIndex == -1) {
                break;
            }
            returnIndex = tempIndex;
        }
        return returnIndex;
    }
}
