package ru.homyakin.seeker.game.event.raid.generator;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;

import java.util.List;

@Component
public class RaidGenerator {

    public List<BattlePersonage> generate(RaidType raidType, LaunchedRaidEvent event, List<BattlePersonage> personages) {
        return generate(raidType, event, personages, SkillFormulaVersion.LEGACY_SKILLS_V1);
    }

    public List<BattlePersonage> generate(
        RaidType raidType,
        LaunchedRaidEvent event,
        List<BattlePersonage> personages,
        SkillFormulaVersion formulaVersion
    ) {
        return generateCalibrated(raidType, event, personages, formulaVersion).enemies();
    }

    public GeneratedPveEnemies generateCalibrated(
        RaidType raidType,
        LaunchedRaidEvent event,
        List<BattlePersonage> personages,
        SkillFormulaVersion formulaVersion
    ) {
        final var raidLevel = event.raidParams().raidLevel();
        if (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3 && (raidLevel < 1 || raidLevel > 50)) {
            throw new IllegalArgumentException("Equipment-scaled raid level must be in 1..50: " + raidLevel);
        }
        var powerBonus = 1.0
            + calcPowerBonusFromLevel(raidLevel);
        final RaidBattlePersonageGenerator generator = switch (raidType) {
            case WOLFPACK -> new WolfPackGenerator();
            case ZOMBIE_HORDE -> new ZombieHordeGenerator();
            case MYCONID_COLONY -> new MyconidColonyGenerator();
            case MAGGEESE_FLOCK -> new MaggeeseFlockGenerator();
        };
        return generator.generateCalibrated(personages, powerBonus, formulaVersion);
    }

    private double calcPowerBonusFromLevel(int raidLevel) {
        return (raidLevel - 10) * 0.06 + 0.14;
    }
}
