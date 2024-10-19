package ru.homyakin.seeker.telegram.group.models;

public record GroupTgId(long value) {
    public static GroupTgId from(long id) {
        return new GroupTgId(id);
    }

    public static GroupTgId from(String id) {
        return new GroupTgId(Long.parseLong(id));
    }
}
