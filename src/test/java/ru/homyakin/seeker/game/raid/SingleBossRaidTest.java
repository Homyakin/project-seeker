package ru.homyakin.seeker.game.raid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.raid.generator.SingleBossGenerator;
import ru.homyakin.seeker.test_utils.CommonUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.TwoPersonageTeamsBattleUtility;

public class SingleBossRaidTest {
    private final SingleBossGenerator generator = new SingleBossGenerator();

    @Disabled
    @Test // TODO после нормального баланса написать новые тесты
    @DisplayName("The probability of winning at raid battle is 50% (+-5)")
    public void victoryAt50Percent() {
        final var repeat = 1000;
        double sumPercent = 0;
        for (int i = 0; i < repeat; ++i) {
            final var personages = PersonageUtils.generateRandom(2);
            final var boss = generator.generate(personages.size());

             sumPercent += TwoPersonageTeamsBattleUtility.probabilityOfFirstTeamWin(
                boss,
                personages
            );
        }
        final var percent = sumPercent / repeat;

        Assertions.assertTrue(
            CommonUtils.compareDoubles(percent, 0.50, 0.05),
            "Expected 50(+-5), actual: " + percent * 100
        );
    }
}
