package ru.homyakin.seeker.game.online.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OnlineStreakTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    @Test
    void effective_whenLastOnlineToday_returnsStoredDays() {
        final var streak = new OnlineStreak(5, at(TODAY));
        Assertions.assertEquals(5, streak.effective(TODAY));
    }

    @Test
    void effective_whenLastOnlineYesterday_returnsStoredDays() {
        final var streak = new OnlineStreak(5, at(TODAY.minusDays(1)));
        Assertions.assertEquals(5, streak.effective(TODAY));
    }

    @Test
    void effective_whenLastOnlineOlder_returnsZero() {
        final var streak = new OnlineStreak(5, at(TODAY.minusDays(2)));
        Assertions.assertEquals(0, streak.effective(TODAY));
    }

    private static LocalDateTime at(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.of(12, 0));
    }
}
