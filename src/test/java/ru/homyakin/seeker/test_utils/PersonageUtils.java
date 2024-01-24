package ru.homyakin.seeker.test_utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

public class PersonageUtils {
    public static List<Personage> randomList(int size) {
        final var personages = new ArrayList<Personage>();
        for (int i = 1; i <= size; ++i) {
            personages.add(withId(PersonageId.from(i)));
        }
        return personages;
    }

    public static Personage random() {
        return new Personage(
            PersonageId.from(RandomUtils.nextLong()),
            RandomStringUtils.randomAlphanumeric(5),
            Money.zero(),
            Characteristics.random(),
            Energy.createDefault(),
            BadgeView.STANDARD
        );
    }

    public static Personage randomZeroEnergy() {
        return new Personage(
            PersonageId.from(RandomUtils.nextLong()),
            RandomStringUtils.randomAlphanumeric(5),
            Money.zero(),
            Characteristics.random(),
            Energy.createZero(TimeUtils.moscowTime()),
            BadgeView.STANDARD
        );
    }

    public static Personage withId(PersonageId id) {
        return new Personage(
            id,
            RandomStringUtils.randomAlphanumeric(5),
            Money.zero(),
            Characteristics.random(),
            Energy.createDefault(),
            BadgeView.STANDARD
        );
    }
}
