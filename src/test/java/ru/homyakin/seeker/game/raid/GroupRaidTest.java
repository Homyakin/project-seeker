package ru.homyakin.seeker.game.raid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.raid.generator.GroupGenerator;
import ru.homyakin.seeker.test_utils.CommonUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.TwoPersonageTeamsBattleUtility;

public class GroupRaidTest {
    private final GroupGenerator generator = new GroupGenerator();

    @Disabled
    @Test // TODO после нормального баланса написать новые тесты
    @DisplayName("The probability of winning at raid battle is 50% (+-5)")
    public void victoryAt50Percent() {
        for (int personageCount = 1; personageCount <= 10; ++personageCount) {
            final var repeat = 1000;
            double sumPercent = 0;
            for (int i = 0; i < repeat; ++i) {
                final var personages = PersonageUtils.generateRandom(personageCount);
                final var enemies = generator.generate(personages.size());

                sumPercent += TwoPersonageTeamsBattleUtility.probabilityOfFirstTeamWin(
                    enemies,
                    personages
                );
            }
            final var percent = sumPercent / repeat;
            System.out.println(personageCount + ": " + percent);
            Assertions.assertTrue(
                CommonUtils.compareDoubles(percent, 0.50, 0.05),
                "Expected 50(+-5), actual: " + percent * 100
            );
        }
    }
}
