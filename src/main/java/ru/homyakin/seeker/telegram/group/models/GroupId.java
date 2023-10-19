package ru.homyakin.seeker.telegram.group.models;

public record GroupId(long value) {
    public static GroupId from(long id) {
        return new GroupId(id);
    }
}
