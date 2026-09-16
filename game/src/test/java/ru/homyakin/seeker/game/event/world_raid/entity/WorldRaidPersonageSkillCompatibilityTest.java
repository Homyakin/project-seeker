package ru.homyakin.seeker.game.event.world_raid.entity;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
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

    private record SkillsDocument(List<WorldRaidPersonage.PersonageSkill> skills) {
    }
}
