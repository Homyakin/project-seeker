package ru.homyakin.seeker.test_utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
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
            new Energy(100, TimeUtils.moscowTime(), Duration.ZERO),
            BadgeView.STANDARD,
            Characteristics.ZERO,
            PersonageEffects.EMPTY,
            Optional.empty()
        );
    }

    public static Personage randomZeroEnergy(Duration timeToRegen) {
        return new Personage(
            PersonageId.from(RandomUtils.nextLong()),
            RandomStringUtils.randomAlphanumeric(5),
            Money.zero(),
            Characteristics.random(),
            new Energy(0, TimeUtils.moscowTime(), timeToRegen),
            BadgeView.STANDARD,
            Characteristics.ZERO,
            PersonageEffects.EMPTY,
            Optional.empty()
        );
    }

    public static Personage withId(PersonageId id) {
        return new Personage(
            id,
            RandomStringUtils.randomAlphanumeric(5),
            Money.zero(),
            Characteristics.random(),
            new Energy(100, TimeUtils.moscowTime(), Duration.ZERO),
            BadgeView.STANDARD,
            new Characteristics(
                ru.homyakin.seeker.utils.RandomUtils.getInInterval(50, 100),
                ru.homyakin.seeker.utils.RandomUtils.getInInterval(30, 60),
                ru.homyakin.seeker.utils.RandomUtils.getInInterval(10, 40),
                0,
                0,
                0
            ),
            PersonageEffects.EMPTY,
            Optional.empty()
        );
    }
}
