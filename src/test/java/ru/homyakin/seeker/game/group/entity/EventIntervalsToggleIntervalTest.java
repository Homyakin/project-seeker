package ru.homyakin.seeker.game.group.entity;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.group.error.ZeroEnabledEventIntervalsError;

public class EventIntervalsToggleIntervalTest {
    @Test
    public void Given_OneEnabledTwoDisabledIntervals_When_ToggleEnabled_Then_ReturnError() {
        // given
        final var intervals = new EventIntervals(
            List.of(
                new EventInterval(1, 2, true),
                new EventInterval(3, 4, false),
                new EventInterval(5, 6, false)
            )
        );

        // when
        final var result = intervals.toggleInterval(0);

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ZeroEnabledEventIntervalsError.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_OneEnabledTwoDisabledIntervals_When_ToggleDisabled_Then_ReturnEventIntervalsWithTwoEnabledIntervals() {
        // given
        final var intervals = new EventIntervals(
            List.of(
                new EventInterval(1, 2, true),
                new EventInterval(3, 4, false),
                new EventInterval(5, 6, false)
            )
        );

        // when
        final var result = intervals.toggleInterval(1);

        // then
        Assertions.assertTrue(result.isRight());
        final var expected = new EventIntervals(
            List.of(
                new EventInterval(1, 2, true),
                new EventInterval(3, 4, true),
                new EventInterval(5, 6, false)
            )
        );
        Assertions.assertEquals(expected, result.get());
    }
}
