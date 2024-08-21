package ru.homyakin.seeker.test_utils.event;

import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.locale.Language;

import java.util.Arrays;

public class EventUtils {
    public static Event randomEvent() {
        final var locale = Arrays.stream(Language.values())
            .map(it -> Map.entry(it, new EventLocale(RandomStringUtils.random(10), RandomStringUtils.random(50))))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        return new Event(
            RandomUtils.nextInt(),
            EventType.RAID,
            locale
        );
    }
}
