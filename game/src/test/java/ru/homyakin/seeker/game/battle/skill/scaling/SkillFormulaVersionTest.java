package ru.homyakin.seeker.game.battle.skill.scaling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SkillFormulaVersionTest {
    @ParameterizedTest
    @EnumSource(SkillFormulaVersion.class)
    void classifiesFormulaFamiliesWithoutReclassifyingHistoricalVersions(SkillFormulaVersion version) {
        Assertions.assertEquals(version != SkillFormulaVersion.LEGACY_SKILLS_V1, version.isScaling());
        Assertions.assertEquals(
            version == SkillFormulaVersion.SCALING_SKILLS_V2 || version == SkillFormulaVersion.SCALING_SKILLS_V3,
            version.isV2OrLater()
        );
    }
}
