package ru.homyakin.seeker.game.event.raid.generator;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;

import java.util.List;

@Component
public class RaidGenerator {

    public List<BattlePersonage> generate(Raid raid, LaunchedRaidEvent event, List<BattlePersonage> personages) {
        return raid.template().generate(personages, calcPowerPercent(event));
    }

    private double calcPowerPercent(LaunchedRaidEvent event) {
        final var raidLevel = event.raidParams().raidLevel();
        final var levelDiff = raidLevel - 10;
        return 1.0 + (levelDiff * 0.05);
    }
}
