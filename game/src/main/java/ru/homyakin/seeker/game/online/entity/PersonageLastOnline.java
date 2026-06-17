package ru.homyakin.seeker.game.online.entity;

import java.time.LocalDateTime;
import java.util.Optional;

import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record PersonageLastOnline(
    PersonageId id,
    String name,
    Optional<String> tag,
    BadgeView badge,
    LocalDateTime lastOnline
) {
}
