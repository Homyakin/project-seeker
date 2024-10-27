package ru.homyakin.seeker.game.group.entity;

import io.vavr.control.Either;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import ru.homyakin.seeker.game.group.error.ZeroEnabledEventIntervalsError;
import ru.homyakin.seeker.game.group.error.IncorrectTimeZone;
import ru.homyakin.seeker.utils.TimeUtils;

public record GroupSettings(
    ZoneOffset timeZone,
    EventIntervals eventIntervals,
    boolean enableToggleHide
) {
    public boolean isActiveForEventNow() {
        return eventIntervals.isActive(TimeUtils.timeWithOffset(timeZone));
    }

    public OffsetDateTime eventDateInNextInterval() {
        return eventIntervals.eventDateInNextInterval(TimeUtils.timeWithOffset(timeZone));
    }

    public Either<ZeroEnabledEventIntervalsError, GroupSettings> toggleEventInterval(int intervalIndex) {
        return eventIntervals.toggleInterval(intervalIndex)
            .map(it -> new GroupSettings(timeZone, it, enableToggleHide));
    }

    public Either<IncorrectTimeZone, GroupSettings> changeTimeZone(int timeZone) {
        if (!validateTimeZone(timeZone)) {
            return Either.left(new IncorrectTimeZone(MIN_TIME_ZONE, MAX_TIME_ZONE));
        }

        try {
            return Either.right(new GroupSettings(ZoneOffset.ofHours(timeZone), eventIntervals, enableToggleHide));
        } catch (DateTimeException _) {
            return Either.left(new IncorrectTimeZone(MIN_TIME_ZONE, MAX_TIME_ZONE));
        }
    }

    private static boolean validateTimeZone(int timeZone) {
        return timeZone >= MIN_TIME_ZONE && timeZone <= MAX_TIME_ZONE;
    }

    private static final int MIN_TIME_ZONE = -12;
    private static final int MAX_TIME_ZONE = 14;
    public static final boolean DEFAULT_ENABLE_TOGGLE_HIDE = true;
}
