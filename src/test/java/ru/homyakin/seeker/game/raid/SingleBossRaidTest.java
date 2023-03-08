package ru.homyakin.seeker.game.raid;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.raid.generator.SingleBossGenerator;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.test_utils.CommonUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.TwoPersonageTeamsBattleUtility;

public class SingleBossRaidTest {
    private final SingleBossGenerator generator = new SingleBossGenerator();

    // @Test TODO после нормального баланса написать новые тесты
    @DisplayName("The probability of winning at raid battle is 50% (+-5)")
    public void victoryAt50Percent() {
        final var personages = PersonageUtils.generateDefault(4);

        final var boss = generator.generate(personages.size());

        final var percent = TwoPersonageTeamsBattleUtility.probabilityOfFirstTeamWin(
            boss,
            personages.stream().map(Personage::toBattlePersonage).toList()
        );

        Assertions.assertTrue(
            CommonUtils.compareDoubles(percent, 0.50, 0.05),
            "Expected 50(+-5), actual: " + percent
        );
    }
}
