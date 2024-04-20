package ru.homyakin.seeker.telegram.group.models;

public record GroupId(long value) {
    public static GroupId from(long id) {
        return new GroupId(id);
    }

    public static GroupId from(String id) {
        return new GroupId(Long.parseLong(id));
    }
}
