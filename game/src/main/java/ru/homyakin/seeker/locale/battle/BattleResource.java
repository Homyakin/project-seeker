package ru.homyakin.seeker.locale.battle;

public record BattleResource(
    String battleStats,
    String battleStatsAttackLine,
    String battleStatsDefenseLine,
    String battleStatsMitigationLine,
    String battleStatsSkillLine,
    String battleStatsSkillsEmpty,
    String battleVisualizerButton,
    String chooseBattlePosition,
    String positionFront,
    String positionMid,
    String positionBack,
    String attackTypeSlash,
    String attackTypeBlunt,
    String attackTypePierce,
    String attackTypeMagical,
    String defenseTypeCloth,
    String defenseTypeLeather,
    String defenseTypePlate,
    String defenseTypeArcane,
    SkillEntry counterAttack,
    SkillEntry thorns,
    SkillEntry doubleAttack,
    SkillEntry berserk,
    SkillEntry hitAndRun,
    SkillEntry bleeding,
    SkillEntry knockback,
    SkillEntry selfHeal,
    SkillEntry preciseStrike,
    SkillEntry retreat,
    SkillEntry feint
) {
    public record SkillEntry(
        String name,
        String first,
        String second,
        String third,
        String fourth,
        String fifth
    ) {
    }
}
