package ru.homyakin.seeker.game.battle.skill.scaling;

public enum SkillFormulaVersion {
    LEGACY_SKILLS_V1,
    SCALING_SKILLS_V1,
    SCALING_SKILLS_V2,
    SCALING_SKILLS_V3,
    ;

    public boolean isScaling() {
        return this != LEGACY_SKILLS_V1;
    }

    public boolean isV2OrLater() {
        return this == SCALING_SKILLS_V2 || this == SCALING_SKILLS_V3;
    }
}
