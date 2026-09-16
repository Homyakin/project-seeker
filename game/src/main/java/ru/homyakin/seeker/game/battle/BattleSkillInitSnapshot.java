package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;

public record BattleSkillInitSnapshot(
    ActiveEnum code,
    int points,
    @JsonProperty(required = false) SkillFormulaVersion formulaVersion
) {
    public BattleSkillInitSnapshot {
        if (formulaVersion == null) {
            formulaVersion = SkillFormulaVersion.LEGACY_SKILLS_V1;
        }
    }

    public BattleSkillInitSnapshot(ActiveEnum code, int points) {
        this(code, points, SkillFormulaVersion.LEGACY_SKILLS_V1);
    }
}
