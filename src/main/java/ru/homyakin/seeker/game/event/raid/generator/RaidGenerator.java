package ru.homyakin.seeker.game.event.raid.generator;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
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
            case ZOMBIE_HORDE -> new ZombieHordeGenerator().generate(personages, powerBonus);
        };
    }

    private double calcPowerBonusFromLevel(int raidLevel) {
        return (raidLevel - 10) * 0.06 + 0.14;
    }
}
