package ru.homyakin.seeker.test_utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.RandomUtils;
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
            PersonageId.from(TestRandom.nextLong()),
            TestRandom.randomAlphanumeric(5),
            Optional.empty(),
            Money.zero(),
            Characteristics.random(),
            new Energy(100, TimeUtils.moscowTime(), Duration.ZERO),
            BadgeView.STANDARD,
            Characteristics.ZERO,
            PersonageEffects.EMPTY
        );
    }

    public static Personage randomZeroEnergy(Duration timeToRegen) {
        return new Personage(
            PersonageId.from(TestRandom.nextLong()),
            TestRandom.randomAlphanumeric(5),
            Optional.empty(),
            Money.zero(),
            Characteristics.random(),
            new Energy(0, TimeUtils.moscowTime(), timeToRegen),
            BadgeView.STANDARD,
            Characteristics.ZERO,
            PersonageEffects.EMPTY
        );
    }

    public static Personage withId(PersonageId id) {
        return new Personage(
            id,
            TestRandom.randomAlphanumeric(5),
            Optional.empty(),
            Money.zero(),
            Characteristics.random(),
            new Energy(100, TimeUtils.moscowTime(), Duration.ZERO),
            BadgeView.STANDARD,
            new Characteristics(
                RandomUtils.getInInterval(50, 100),
                RandomUtils.getInInterval(30, 60),
                RandomUtils.getInInterval(10, 40),
                0,
                0,
                0
            ),
            PersonageEffects.EMPTY
        );
    }
}
