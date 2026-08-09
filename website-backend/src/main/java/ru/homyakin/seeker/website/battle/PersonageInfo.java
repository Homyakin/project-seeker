package ru.homyakin.seeker.website.battle;

public record PersonageInfo(long id, String displayName) {
    public static String formatDisplayName(String tag, String name) {
        return GroupInfo.formatDisplayName(tag, name);
    }
}
