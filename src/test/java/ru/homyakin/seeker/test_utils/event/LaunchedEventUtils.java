package ru.homyakin.seeker.test_utils.event;

import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;

public class LaunchedEventUtils {
    public static LaunchedEvent withId(int id) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            id,
            TestRandom.nextInt(),
            now,
            now.plus(Duration.ofSeconds(RandomUtils.getInInterval(3601, 7199))),
            EventStatus.LAUNCHED
        );
    }

    public static LaunchedEvent withEventId(int eventId) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            TestRandom.nextLong(),
            eventId,
            now,
            now.plus(Duration.ofSeconds(RandomUtils.getInInterval(3601, 7199))),
            EventStatus.LAUNCHED
        );
    }

    public static LaunchedEvent expiredWithEventId(int eventId) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            TestRandom.nextLong(),
            eventId,
            now.minus(Duration.ofSeconds(RandomUtils.getInInterval(3601, 7199))),
            now,
            EventStatus.EXPIRED
        );
    }
}
