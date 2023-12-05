package ru.homyakin.seeker.test_utils;

import java.util.ArrayList;
import java.util.List;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public class PersonageUtils {
    public static List<Personage> generateRandom(int size) {
        final var personages = new ArrayList<Personage>();
        for (int i = 1; i <= size; ++i) {
            personages.add(
                new Personage(
                    PersonageId.from(i),
                    "",
                    Money.zero(),
                    Characteristics.random(),
                    Energy.createDefault()
                )
            );
        }
        return personages;
    }
}
