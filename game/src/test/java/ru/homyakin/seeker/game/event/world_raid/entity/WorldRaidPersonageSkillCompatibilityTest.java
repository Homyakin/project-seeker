package ru.homyakin.seeker.game.event.world_raid.entity;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.utils.JsonUtils;

class WorldRaidPersonageSkillCompatibilityTest {
    @Test
    void oldJsonAndTwoArgumentConstructorDefaultToLegacyVersion() {
        final var jsonUtils = new JsonUtils();
        final var fromJson = jsonUtils.fromString(
            """
                {"activeEnum":"SELF_HEAL","rank":"FIFTH"}
                """,
            WorldRaidPersonage.PersonageSkill.class
        );
        final var fromConstructor = new WorldRaidPersonage.PersonageSkill(
            ActiveEnum.SELF_HEAL,
            SkillRank.FIFTH
        );

        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, fromJson.version());
        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, fromConstructor.version());
    }

    @Test
    void oldTomlDefaultsToLegacyAndExplicitTomlKeepsScalingVersion() throws Exception {
        final var document = TomlMapper.builder().build().readValue(
            """
                [[skills]]
                activeEnum = "THORNS"
                rank = "THIRD"

                [[skills]]
                activeEnum = "TEMPO_BREAK"
                rank = "FOURTH"
                version = "SCALING_SKILLS_V1"
                """,
            SkillsDocument.class
        );

        Assertions.assertEquals(2, document.skills().size());
        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, document.skills().getFirst().version());
        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V1, document.skills().getLast().version());
    }

    @Test
    void scalingVersionSurvivesJsonRoundTrip() throws Exception {
        final var jsonUtils = new JsonUtils();
        final var expected = new WorldRaidPersonage.PersonageSkill(
            ActiveEnum.ACCUMULATION,
            SkillRank.SECOND,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );

        final var json = jsonUtils.mapToPostgresJson(expected).getValue();
        final var actual = jsonUtils.fromString(json, WorldRaidPersonage.PersonageSkill.class);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void scalingV2SurvivesFullWorldRaidJsonRoundTripAndBattleMaterialization() throws Exception {
        final var expected = new WorldRaidPersonage(
            List.of(new WorldRaidPersonage.PersonageSkill(
                ActiveEnum.PENETRATION,
                SkillRank.FOURTH,
                SkillFormulaVersion.SCALING_SKILLS_V2
            )),
            1_200,
            17,
            9,
            1.75,
            340,
            23,
            List.of(new WorldRaidPersonage.AttackTemplate(AttackType.PIERCE, 3, 140)),
            List.of(new WorldRaidPersonage.DefenseTemplate(DefenseType.LEATHER, 75)),
            Position.BACK
        );
        final var jsonUtils = new JsonUtils();

        final var json = jsonUtils.mapToPostgresJson(expected).getValue();
        final var restored = jsonUtils.fromString(json, WorldRaidPersonage.class);
        final var battlePersonage = new BattlePersonage(restored, restored.position());
        final var skillSnapshot = battlePersonage.skillSnapshots().getFirst();

        Assertions.assertAll(
            () -> Assertions.assertEquals(expected, restored),
            () -> Assertions.assertTrue(json.contains("\"version\":\"SCALING_SKILLS_V2\"")),
            () -> Assertions.assertEquals(ActiveEnum.PENETRATION, skillSnapshot.code()),
            () -> Assertions.assertEquals(SkillRank.FOURTH.requiredPoints(), skillSnapshot.points()),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V2,
                skillSnapshot.formulaVersion()
            )
        );
    }

    private record SkillsDocument(List<WorldRaidPersonage.PersonageSkill> skills) {
    }
}
