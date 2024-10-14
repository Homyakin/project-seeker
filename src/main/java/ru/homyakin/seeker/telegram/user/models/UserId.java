package ru.homyakin.seeker.telegram.user.models;

public record UserId(long value) {
    public static UserId from(long id) {
        return new UserId(id);
    }

    public static UserId from(String id) {
        return new UserId(Long.parseLong(id));
    }
}
