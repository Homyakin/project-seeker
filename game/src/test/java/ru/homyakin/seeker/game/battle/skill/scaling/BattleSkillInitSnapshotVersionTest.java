package ru.homyakin.seeker.game.battle.skill.scaling;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattleSkillInitSnapshot;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

class BattleSkillInitSnapshotVersionTest {
    @Test
    void twoArgumentConstructorUsesLegacyVersion() {
        final var snapshot = new BattleSkillInitSnapshot(ActiveEnum.BERSERK, 4);

        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, snapshot.formulaVersion());
    }

    @Test
    void nullVersionUsesLegacyVersion() {
        final var snapshot = new BattleSkillInitSnapshot(ActiveEnum.BERSERK, 4, null);

        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, snapshot.formulaVersion());
    }

    @Test
    void missingJsonVersionUsesLegacyVersion() throws Exception {
        final var snapshot = new ObjectMapper().readValue(
            "{\"code\":\"BERSERK\",\"points\":4}",
            BattleSkillInitSnapshot.class
        );

        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, snapshot.formulaVersion());
    }

    @Test
    void preservesExplicitScalingVersion() {
        final var snapshot = new BattleSkillInitSnapshot(
            ActiveEnum.BERSERK,
            4,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );

        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V1, snapshot.formulaVersion());
    }

    @Test
    void preservesExplicitSecondScalingVersion() {
        final var snapshot = new BattleSkillInitSnapshot(
            ActiveEnum.BERSERK,
            4,
            SkillFormulaVersion.SCALING_SKILLS_V2
        );

        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V2, snapshot.formulaVersion());
    }
}
