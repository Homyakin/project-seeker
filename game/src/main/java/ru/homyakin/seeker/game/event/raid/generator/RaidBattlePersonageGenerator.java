package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;

import java.util.List;

public interface RaidBattlePersonageGenerator {
    default List<BattlePersonage> generate(List<BattlePersonage> personages, double powerBonus) {
        return generate(personages, powerBonus, SkillFormulaVersion.LEGACY_SKILLS_V1);
    }

    default List<BattlePersonage> generate(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    ) {
        return generateCalibrated(personages, powerBonus, formulaVersion).enemies();
    }

    GeneratedPveEnemies generateCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    );

    static void validateInputs(List<BattlePersonage> personages, double powerBonus, SkillFormulaVersion formulaVersion) {
        if (personages == null || personages.isEmpty()) {
            throw new IllegalArgumentException("Raid generation requires at least one participant");
        }
        if (!Double.isFinite(powerBonus) || powerBonus < 0) {
            throw new IllegalArgumentException("Raid power bonus must be finite and nonnegative");
        }
        if (formulaVersion == null) {
            throw new IllegalArgumentException("Raid skill formula version must be specified");
        }
    }
}
