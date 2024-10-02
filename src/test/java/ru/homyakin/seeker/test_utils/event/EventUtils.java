package ru.homyakin.seeker.test_utils.event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.RaidLocale;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.test_utils.TestRandom;

public class EventUtils {
    public static Raid randomRaid() {
        final var locale = Arrays.stream(Language.values())
            .map(it -> Map.entry(it, new RaidLocale(TestRandom.random(10), TestRandom.random(50))))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        return new Raid(
            TestRandom.nextInt(),
            TestRandom.randomAlphanumeric(10),
            RaidTemplate.ENEMY_GROUP,
            locale
        );
    }
}
