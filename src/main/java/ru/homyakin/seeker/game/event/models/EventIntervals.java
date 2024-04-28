package ru.homyakin.seeker.game.event.models;

import io.vavr.control.Either;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.Pair;

public record EventIntervals(
    // Интервалы не пересекаются
    List<EventInterval> intervals
) {
    public boolean isActive(OffsetDateTime checkTime) {
        final var currentHour = checkTime.getHour();
        final var enabledIntervals = intervals
            .stream()
            .filter(EventInterval::isEnabled)
            .toList();
        if (enabledIntervals.isEmpty()) {
            logger.warn("There are no enabled event intervals to check active {}, return true", this);
            return true;
        }
        for (final var interval : enabledIntervals) {
            if (interval.startHour() <= currentHour && interval.endHour() > currentHour) {
                return true;
            }
        }
        return false;
    }

    public OffsetDateTime eventDateInNextInterval(OffsetDateTime current) {
        final var enabledIntervals = intervals
            .stream()
            .filter(EventInterval::isEnabled)
            .toList();
        if (enabledIntervals.isEmpty()) {
            logger.warn("There are no enabled event intervals to calculate next date {}, return next day", this);
            return current.plusDays(1);
        }

        final var result = getNextEventInterval(current, enabledIntervals);
        final var nextInterval = result.first();
        final var isNextIntervalToday = result.second();

        final var startTime = atStartOfHour(current, nextInterval.startHour());
        // 5 минут отнимается, чтобы шедулер успел выбрать запись и проверить её актуальность
        // иначе придётся ждать следующего интервала
        final var endTime = atStartOfHour(current, nextInterval.endHour()).minusMinutes(5);

        final var nextDate = RandomUtils.getInInterval(startTime, endTime);
        if (isNextIntervalToday) {
            return nextDate;
        } else {
            return nextDate.plusDays(1);
        }
    }

    public Either<ZeroEnabledEventIntervalsError, EventIntervals> toggleInterval(int intervalIndex) {
        final var intervals = new ArrayList<EventInterval>();
        int enabledIntervals = 0;
        for (int i = 0; i < this.intervals().size(); ++i) {
            if (intervalIndex == i) {
                intervals.add(this.intervals().get(i).toggle());
            } else {
                intervals.add(this.intervals().get(i));
            }
            if (intervals.get(i).isEnabled()) {
                ++enabledIntervals;
            }
        }
        if (enabledIntervals == 0) {
            return Either.left(ZeroEnabledEventIntervalsError.INSTANCE);
        }
        return Either.right(new EventIntervals(intervals));
    }

    private OffsetDateTime atStartOfHour(OffsetDateTime dateTime, int hour) {
        return dateTime.withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Для корректной работы этой логики важно, чтобы интервалы не пересекались
     */
    private Pair<EventInterval, Boolean> getNextEventInterval(OffsetDateTime current, List<EventInterval> enabledIntervals) {
        final var currentHour = current.getHour();
        EventInterval nextInterval = null;
        int minDiff = Integer.MAX_VALUE;
        var isNextIntervalToday = true;
        for (final var interval : enabledIntervals) {
            var isTodayInterval = true;
            var diff = interval.startHour() - currentHour;
            // Если интервал уже прошёл, значит он будет только через 24 часа
            if (diff <= 0) {
                isTodayInterval = false;
                diff += 24;
            }
            if (diff < minDiff) {
                isNextIntervalToday = isTodayInterval;
                minDiff = diff;
                nextInterval = interval;
            }
        }
        if (nextInterval == null) {
            throw new NoSuchElementException("There is no event interval to calculate next event");
        }
        return Pair.of(nextInterval, isNextIntervalToday);
    }

    private static final Logger logger = LoggerFactory.getLogger(EventIntervals.class);
}
