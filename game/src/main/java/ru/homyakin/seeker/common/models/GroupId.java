package ru.homyakin.seeker.common.models;

public record GroupId(long value) {
    public static GroupId from(long id) {
        return new GroupId(id);
    }
}
