package ru.homyakin.seeker.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
}
