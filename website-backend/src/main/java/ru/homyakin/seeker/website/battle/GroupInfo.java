package ru.homyakin.seeker.website.battle;

public record GroupInfo(long id, String displayName) {
    public static String formatDisplayName(String tag, String name) {
        if (tag != null && !tag.isBlank()) {
            return "[" + tag + "] " + name;
        }
        return name;
    }
}
