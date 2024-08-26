package ru.homyakin.seeker.test_utils.event;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.random.UniformRandomGenerator;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;

public class LaunchedEventUtils {
    public static LaunchedEvent withId(int id) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            id,
            RandomUtils.nextInt(),
            now,
            now.plus(Duration.ofSeconds(RandomUtils.nextInt(3601, 7199))),
            EventStatus.LAUNCHED
        );
    }

    public static LaunchedEvent withEventId(int eventId) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            RandomUtils.nextLong(),
            eventId,
            now,
            now.plus(Duration.ofSeconds(RandomUtils.nextInt(3601, 7199))),
            EventStatus.LAUNCHED
        );
    }

    public static LaunchedEvent expiredWithEventId(int eventId) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            RandomUtils.nextLong(),
            eventId,
            now.minus(Duration.ofSeconds(RandomUtils.nextInt(3601, 7199))),
            now,
            EventStatus.EXPIRED
        );
    }
}
