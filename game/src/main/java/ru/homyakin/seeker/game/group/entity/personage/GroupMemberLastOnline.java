package ru.homyakin.seeker.game.group.entity.personage;

import java.time.LocalDateTime;
import java.util.Optional;

import ru.homyakin.seeker.game.online.entity.OnlineStreak;

public record GroupMemberLastOnline(
    LocalDateTime personageLastOnline,
    Optional<LocalDateTime> membershipLastOnline,
    Optional<OnlineStreak> membershipStreak
) {
}
