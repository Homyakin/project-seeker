package ru.homyakin.seeker.game.group.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventIntervalsEventDateInNextIntervalTest {
    private final LocalDate date = LocalDate.of(2007, 1, 1);
    private final ZoneOffset offset = ZoneOffset.UTC;

    @Test
    public void When_NoEnabledIntervals_Then_ReturnDatePlusDay() {
        // given
        final var intervals = new EventIntervals(List.of(new EventInterval(1, 2, false)));
        final var dateTime = OffsetDateTime.now(offset);

        // when
        final var result = intervals.eventDateInNextInterval(dateTime);

        // then
        Assertions.assertEquals(dateTime.plusDays(1), result);
    }

    @Test
    public void Given_TwoEnabledIntervals_When_DateIsInFirstInterval_Then_ReturnDateInSameDayInSecondInterval() {
        // given
        final var intervals = new EventIntervals(
            List.of(
                new EventInterval(1, 2, true),
                new EventInterval(3, 4, true)
            )
        );
        final var dateTime = OffsetDateTime.of(date, LocalTime.of(1,2, 0), offset);

        // when
        final var result = intervals.eventDateInNextInterval(dateTime);

        // then
        final var lowBound =  OffsetDateTime.of(date, LocalTime.of(3,0, 0), offset);
        final var upBound =  OffsetDateTime.of(date, LocalTime.of(3,55, 0), offset);
        Assertions.assertTrue(lowBound.isBefore(result) || lowBound.isEqual(result));
        Assertions.assertTrue(upBound.isAfter(result) || upBound.isEqual(result));
    }

    @Test
    public void Given_TwoEnabledIntervals_When_DateIsInSecondInterval_Then_ReturnDateInNextDayInFirstInterval() {
        // given
        final var intervals = new EventIntervals(
            List.of(
                new EventInterval(1, 3, true),
                new EventInterval(4, 5, true)
            )
        );
        final var dateTime = OffsetDateTime.of(date, LocalTime.of(4,2, 0), offset);

        // when
        final var result = intervals.eventDateInNextInterval(dateTime);

        // then
        final var lowBound =  OffsetDateTime.of(date.plusDays(1), LocalTime.of(1,0, 0), offset);
        final var upBound =  OffsetDateTime.of(date.plusDays(1), LocalTime.of(2,55, 0), offset);
        Assertions.assertTrue(lowBound.isBefore(result) || lowBound.isEqual(result));
        Assertions.assertTrue(upBound.isAfter(result) || upBound.isEqual(result));
    }

    @Test
    public void Given_OneEnabledIntervals_When_DateIsInInterval_Then_ReturnDateInNextDayInInterval() {
        // given
        final var intervals = new EventIntervals(
            List.of(
                new EventInterval(1, 3, true)
            )
        );
        final var dateTime = OffsetDateTime.of(date, LocalTime.of(2,2, 0), offset);

        // when
        final var result = intervals.eventDateInNextInterval(dateTime);

        // then
        final var lowBound =  OffsetDateTime.of(date.plusDays(1), LocalTime.of(1,0, 0), offset);
        final var upBound =  OffsetDateTime.of(date.plusDays(1), LocalTime.of(2,55, 0), offset);
        Assertions.assertTrue(lowBound.isBefore(result) || lowBound.isEqual(result));
        Assertions.assertTrue(upBound.isAfter(result) || upBound.isEqual(result));
    }
}
