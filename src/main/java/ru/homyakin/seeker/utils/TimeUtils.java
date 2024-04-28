package ru.homyakin.seeker.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class TimeUtils {
    public static LocalDateTime moscowTime() {
        return LocalDateTime.now(moscowZone());
    }

    public static LocalDate moscowDate() {
        return LocalDate.now(moscowZone());
    }

    public static ZoneId moscowZone() {
        return ZoneId.of("Europe/Moscow");
    }

    public static ZoneOffset moscowOffset() {
        return ZoneOffset.of("+03:00");
    }

    public static OffsetDateTime timeWithOffset(ZoneOffset offset) {
        return LocalDateTime.now(offset).atOffset(offset);
    }

    public static String toString(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }

    public static LocalDate thisWeekMonday() {
        final var today = LocalDate.now();
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate thisWeekSunday() {
        final var today = LocalDate.now();
        return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy ('GMT'+03)");
}
