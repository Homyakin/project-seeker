package ru.homyakin.seeker.test_utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;

public class PersonageUtils {
    public static List<Personage> generateDefault(int size) {
        final var personages = new ArrayList<Personage>();
        for (int i = 1; i <= size; ++i) {
            personages.add(
                new Personage(
                    i,
                    "",
                    Money.zero(),
                    Characteristics.createDefault(),
                    LocalDateTime.now()
                )
            );
        }
        return personages;
    }
}
