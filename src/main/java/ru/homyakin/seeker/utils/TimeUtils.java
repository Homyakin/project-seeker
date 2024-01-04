package ru.homyakin.seeker.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    public static String toString(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy ('GMT'+03)");
}
