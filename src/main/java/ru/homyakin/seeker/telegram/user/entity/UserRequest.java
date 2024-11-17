package ru.homyakin.seeker.telegram.user.entity;

import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public record UserRequest(
    UserId id,
    Optional<String> originalUsername,
    Optional<Username> username
) {
}
