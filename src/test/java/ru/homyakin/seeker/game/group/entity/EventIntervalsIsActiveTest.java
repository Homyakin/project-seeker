package ru.homyakin.seeker.game.group.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.utils.TimeUtils;

public class EventIntervalsIsActiveTest {
    @Test
    public void When_NoEnabledIntervals_Then_ReturnTrue() {
        // given
        final var intervals = new EventIntervals(
            List.of(new EventInterval(1, 2, false))
        );

        // when
        final var result = intervals.isActive(TimeUtils.timeWithOffset(ZoneOffset.UTC));

        // then
        Assertions.assertTrue(result);
    }

    @Test
    public void Given_EnabledInterval_When_TimeNotInEnabledInterval_Then_ReturnFalse() {
        // given
        final var offset = ZoneOffset.of("+01:00");
        final var intervals = new EventIntervals(
            List.of(new EventInterval(1, 2, true))
        );

        // when
        final var result = intervals.isActive(
            OffsetDateTime.of(1, 1, 1, 2, 0, 0, 0, offset)
        );

        // then
        Assertions.assertFalse(result);
    }

    @Test
    public void Given_EnabledInterval_When_TimeInEnabledInterval_Then_ReturnTrue() {
        // given
        final var offset = ZoneOffset.of("+01:00");
        final var intervals = new EventIntervals(
            List.of(new EventInterval(1, 2, true))
        );

        // when
        final var result = intervals.isActive(
            OffsetDateTime.of(1, 1, 1, 1, 30, 0, 0, offset)
        );

        // then
        Assertions.assertTrue(result);
    }
}
