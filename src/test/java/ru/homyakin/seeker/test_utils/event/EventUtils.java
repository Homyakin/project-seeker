package ru.homyakin.seeker.test_utils.event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.RaidLocale;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.locale.Language;

public class EventUtils {
    public static Raid randomRaid() {
        final var locale = Arrays.stream(Language.values())
            .map(it -> Map.entry(it, new RaidLocale(RandomStringUtils.random(10), RandomStringUtils.random(50))))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        return new Raid(
            RandomUtils.nextInt(),
            RandomStringUtils.randomAlphanumeric(10),
            RaidTemplate.ENEMY_GROUP,
            locale
        );
    }
}
