package ru.homyakin.seeker.telegram.user.entity;

public record Username(
    String value
) {
    public Username(String value) {
        this.value = value.toLowerCase();
    }

    public static Username from(String username) {
        return new Username(username);
    }
}
