package ru.homyakin.seeker.telegram.group.models;

import io.vavr.control.Either;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record ActiveTime(
    int startHour,
    int endHour,
    int timeZone
) {
    public static ActiveTime createDefault() {
        return new ActiveTime(0, 24, 3);
    }

    public static Either<ActiveTimeError, ActiveTime> from(int startHour, int endHour, int timeZone) {
        if (!validateHour(startHour) || !validateHour(endHour)) {
            return Either.left(ActiveTimeError.IncorrectHour.INSTANCE);
        }

        if (startHour > endHour) {
            return Either.left(ActiveTimeError.StartMoreThanEnd.INSTANCE);
        }

        if (!validateTimeZone(timeZone)) {
            return Either.left(new ActiveTimeError.IncorrectTimeZone(MIN_TIME_ZONE, MAX_TIME_ZONE));
        }

        return Either.right(new ActiveTime(startHour, endHour, timeZone));
    }

    public boolean isActiveNow() {
        final var zoneId = ZoneId.ofOffset("", ZoneOffset.ofHours(timeZone));
        final var currentTime = ZonedDateTime.now(zoneId).toLocalTime();

        final var startTime = LocalTime.of(startHour, 0);
        LocalTime endTime;
        if (endHour == 24) {
            endTime = LocalTime.of(23, 59, 59);
        } else {
            endTime = LocalTime.of(endHour, 0);
        }
        return currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
            || currentTime.equals(startTime)
            || currentTime.equals(endTime);
    }

    public boolean isHourInInterval(int hour) {
        return hour >= startHour && hour <= endHour;
    }

    @Override
    public String toString() {
        String startHourStr = String.format("%02d:00", startHour);
        String endHourStr = String.format("%02d:00", endHour);
        String timeZoneStr = String.format("%+d", timeZone);
        return startHourStr + " - " + endHourStr + " " + timeZoneStr;
    }

    private static boolean validateHour(int hour) {
        return hour >= 0 && hour <= 24;
    }

    private static boolean validateTimeZone(int timeZone) {
        return timeZone >= MIN_TIME_ZONE && timeZone <= MAX_TIME_ZONE;
    }

    private static final int MIN_TIME_ZONE = -12;
    private static final int MAX_TIME_ZONE = 14;
}
