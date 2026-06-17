package ru.homyakin.seeker.game.group.entity.personage;

import java.time.LocalDateTime;
import java.util.Optional;

public record GroupMemberLastOnline(
    LocalDateTime personageLastOnline,
    Optional<LocalDateTime> membershipLastOnline
) {
}
