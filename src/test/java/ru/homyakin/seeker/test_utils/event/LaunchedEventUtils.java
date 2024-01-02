package ru.homyakin.seeker.test_utils.event;

import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

public class LaunchedEventUtils {
    public static LaunchedEvent fromEvent(Event event) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            RandomUtils.nextLong(),
            event.id(),
            now,
            now.plus(event.duration()),
            EventStatus.LAUNCHED
        );
    }

    public static LaunchedEvent expiredFromEvent(Event event) {
        final var now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            RandomUtils.nextLong(),
            event.id(),
            now.minus(event.duration()),
            now,
            EventStatus.EXPIRED
        );
    }
}
