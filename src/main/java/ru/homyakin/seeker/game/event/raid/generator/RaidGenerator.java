package ru.homyakin.seeker.game.event.raid.generator;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;

import java.util.List;

@Component
public class RaidGenerator {

    public List<BattlePersonage> generate(RaidType raidType, LaunchedRaidEvent event, List<BattlePersonage> personages) {
        var powerBonus = 1.0
            + calcPowerBonusFromLevel(event.raidParams().raidLevel());
        return switch (raidType) {
            case WOLFPACK -> new WolfPackGenerator().generate(personages, powerBonus);
        };
    }

    private double calcPowerBonusFromLevel(int raidLevel) {
        final var levelDiff = raidLevel - 10;
        return levelDiff * 0.1;
    }
}
