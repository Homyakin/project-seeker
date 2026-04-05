package ru.homyakin.seeker.game.group.entity;

import java.time.LocalDateTime;
import java.util.Optional;

public record GroupTaxInfo(
    int effectiveTax,
    int memberCount,
    Optional<LocalDateTime> lastTaxUpdate
) {
}
