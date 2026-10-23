package ru.homyakin.seeker.locale.battle;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.locale.help.HelpLocalization;

class VersionedSkillHelpLocalizationTest {
    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void everyScalingSkillHasARealGeneralDescriptionWithoutAnExampleRank() {
        Assertions.assertEquals(15, ActiveEnum.values().length);
        for (final var language : List.of(Language.RU, Language.ES)) {
            for (final var version : List.of(SkillFormulaVersion.SCALING_SKILLS_V1, SkillFormulaVersion.SCALING_SKILLS_V2)) {
                for (final var skill : ActiveEnum.values()) {
                    final var description = BattleLocalization.skillGeneralDescription(language, skill, version);
                    final var help = HelpLocalization.battleSkill(language, skill, version);
                    Assertions.assertNotNull(description, skill.name());
                    Assertions.assertFalse(description.isBlank(), skill.name());
                    Assertions.assertTrue(help.contains(description), skill.name());
                    Assertions.assertFalse(help.contains("${"), help);
                    Assertions.assertFalse(help.contains("null"), help);
                    Assertions.assertFalse(description.contains("оч.)"), description);
                    Assertions.assertFalse(description.contains("пример"), description);
                }
            }
        }
    }

    @Test
    void secondVersionExplainsDeterministicMobileAttackAndMainBleedingType() {
        final var mobile = HelpLocalization.battleSkill(Language.RU, ActiveEnum.HIT_AND_RUN, SkillFormulaVersion.SCALING_SKILLS_V2);
        final var bleeding = HelpLocalization.battleSkill(Language.RU, ActiveEnum.BLEEDING, SkillFormulaVersion.SCALING_SKILLS_V2);
        Assertions.assertAll(
            () -> Assertions.assertTrue(mobile.contains("Когда умение готово")),
            () -> Assertions.assertTrue(mobile.contains("дальностью +1")),
            () -> Assertions.assertTrue(mobile.contains("после неё отступает")),
            () -> Assertions.assertFalse(mobile.contains("с шансом")),
            () -> Assertions.assertFalse(mobile.contains("В начале хода")),
            () -> Assertions.assertTrue(bleeding.contains("основного типа атаки")),
            () -> Assertions.assertFalse(bleeding.contains("рубящий"))
        );
    }

    @Test
    void guardPenetrationAndTempoGeneralDescriptionsMatchTheSelectedVersion() {
        final var version = SkillFormulaVersion.SCALING_SKILLS_V2;
        final var guard = HelpLocalization.battleSkill(Language.RU, ActiveEnum.GUARD, version);
        final var penetration = HelpLocalization.battleSkill(Language.RU, ActiveEnum.PENETRATION, version);
        final var tempo = HelpLocalization.battleSkill(Language.RU, ActiveEnum.TEMPO_BREAK, version);
        Assertions.assertAll(
            () -> Assertions.assertTrue(guard.contains("ровно на одну линию позади")),
            () -> Assertions.assertTrue(guard.contains("не прикрывает другого владельца Заслона")),
            () -> Assertions.assertTrue(penetration.contains("ещё на две обычные попытки")),
            () -> Assertions.assertTrue(penetration.contains("без повторного дополнительного урона")),
            () -> Assertions.assertTrue(tempo.contains("один собственный ход")),
            () -> Assertions.assertTrue(HelpLocalization.battleSkill(
                Language.RU, ActiveEnum.TEMPO_BREAK, SkillFormulaVersion.SCALING_SKILLS_V1
            ).contains("два собственных хода")),
            () -> Assertions.assertFalse(HelpLocalization.battleSkill(
                Language.RU, ActiveEnum.PENETRATION, SkillFormulaVersion.SCALING_SKILLS_V1
            ).contains("ещё на две обычные попытки"))
        );
    }

    @Test
    void legacyHelpAndRankedHistoricalDescriptionsAreUnchanged() {
        for (final var skill : ActiveEnum.values()) {
            Assertions.assertEquals(
                BattleLocalization.skillGeneralDescription(Language.RU, skill),
                BattleLocalization.skillGeneralDescription(Language.RU, skill, SkillFormulaVersion.LEGACY_SKILLS_V1)
            );
            Assertions.assertEquals(
                HelpLocalization.battleSkill(Language.RU, skill),
                HelpLocalization.battleSkill(Language.RU, skill, SkillFormulaVersion.LEGACY_SKILLS_V1)
            );
            Assertions.assertEquals(
                BattleLocalization.skillDescription(Language.RU, skill, 4),
                BattleLocalization.skillDescription(Language.RU, skill, 4, SkillFormulaVersion.LEGACY_SKILLS_V1)
            );
        }
        Assertions.assertTrue(HelpLocalization.battleSkill(Language.RU, ActiveEnum.HIT_AND_RUN).contains("В начале хода с шансом"));
        Assertions.assertTrue(HelpLocalization.battleSkill(Language.RU, ActiveEnum.BLEEDING).contains("рубящий урон"));
        Assertions.assertTrue(BattleLocalization.skillDescription(Language.RU, ActiveEnum.DOUBLE_ATTACK, 3)
            .contains("15 дополнительного урона"));
    }
}
