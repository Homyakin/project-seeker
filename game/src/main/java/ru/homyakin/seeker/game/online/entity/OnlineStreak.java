package ru.homyakin.seeker.game.online.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OnlineStreak(
    int days,
    LocalDateTime lastOnline
) {
    /**
     * Streak still counts if last activity was today or yesterday (Moscow calendar);
     * otherwise display 0 until the next online day starts a new streak.
     */
    public int effective(LocalDate today) {
        final var lastDate = lastOnline.toLocalDate();
        if (lastDate.equals(today) || lastDate.equals(today.minusDays(1))) {
            return days;
        }
        return 0;
    }
}
