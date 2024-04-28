package ru.homyakin.seeker.telegram.group.models;

import io.vavr.control.Either;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import ru.homyakin.seeker.game.event.models.EventIntervals;
import ru.homyakin.seeker.game.event.models.ZeroEnabledEventIntervalsError;
import ru.homyakin.seeker.utils.TimeUtils;

public record GroupSettings(
    ZoneOffset timeZone,
    EventIntervals eventIntervals
) {
    public boolean isActiveForEventNow() {
        return eventIntervals.isActive(TimeUtils.timeWithOffset(timeZone));
    }

    public OffsetDateTime eventDateInNextInterval() {
        return eventIntervals.eventDateInNextInterval(TimeUtils.timeWithOffset(timeZone));
    }

    public Either<ZeroEnabledEventIntervalsError, GroupSettings> toggleEventInterval(int intervalIndex) {
        return eventIntervals.toggleInterval(intervalIndex)
            .map(it -> new GroupSettings(timeZone, it));
    }

    public Either<IncorrectTimeZone, GroupSettings> changeTimeZone(int timeZone) {
        if (!validateTimeZone(timeZone)) {
            return Either.left(new IncorrectTimeZone(MIN_TIME_ZONE, MAX_TIME_ZONE));
        }

        try {
            return Either.right(new GroupSettings(ZoneOffset.ofHours(timeZone), eventIntervals));
        } catch (DateTimeException _) {
            return Either.left(new IncorrectTimeZone(MIN_TIME_ZONE, MAX_TIME_ZONE));
        }
    }

    private static boolean validateTimeZone(int timeZone) {
        return timeZone >= MIN_TIME_ZONE && timeZone <= MAX_TIME_ZONE;
    }

    private static final int MIN_TIME_ZONE = -12;
    private static final int MAX_TIME_ZONE = 14;
}
