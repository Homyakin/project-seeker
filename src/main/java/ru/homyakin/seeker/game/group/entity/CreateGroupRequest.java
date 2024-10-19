package ru.homyakin.seeker.game.group.entity;

import java.time.LocalDateTime;

public record CreateGroupRequest(
    boolean isActive,
    LocalDateTime initDate,
    LocalDateTime nextEventDate,
    LocalDateTime nextRumorDate,
    GroupSettings settings
) {
}
