package ru.homyakin.seeker.game.event.raid.generator;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;

import java.util.List;

@Component
public class RaidGenerator {

    public List<BattlePersonage> generate(Raid raid, LaunchedRaidEvent event, List<BattlePersonage> personages) {
        var powerBonus = 1.0
            + calcPowerBonusFromLevel(event.raidParams().raidLevel())
            + calcPowerBonusFromPersonages(personages.size());
        return raid.template().generate(personages, powerBonus);
    }

    private double calcPowerBonusFromLevel(int raidLevel) {
        final var levelDiff = raidLevel - 10;
        return levelDiff * 0.1;
    }

    private double calcPowerBonusFromPersonages(int count) {
        if (count < 3) {
            return 0.1; // Мотивация не делать прокси группы, минимум 3 человека
        } else {
            return 0;
        }
    }
}
