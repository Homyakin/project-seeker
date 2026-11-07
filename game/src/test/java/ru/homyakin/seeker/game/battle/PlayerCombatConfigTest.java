package ru.homyakin.seeker.game.battle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;

class PlayerCombatConfigTest {
    @Test
    void defaultsPreserveLegacyCombatAndBaselineTempo() {
        final var config = new PlayerCombatConfig();

        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, config.version());
        Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, config.tempoVersion());
    }

    @Test
    void unspecifiedTempoDoesNotChangeHistoricalScalingRules() {
        final var config = new PlayerCombatConfig();

        for (final var version : SkillFormulaVersion.values()) {
            config.setVersion(version);
            Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, config.tempoVersion());
        }
    }

    @Test
    void nullTempoIsRejectedWithoutChangingConfiguration() {
        final var config = new PlayerCombatConfig();

        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setTempoVersion(null));
        Assertions.assertEquals(BattleTempoVersion.BASELINE_V1, config.tempoVersion());
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V2", "SCALING_SKILLS_V3"},
        mode = EnumSource.Mode.EXCLUDE)
    void acceleratedTempoCannotBeUsedWithHistoricalOrLegacyPlayerRules(SkillFormulaVersion version) {
        final var config = new PlayerCombatConfig();
        config.setVersion(version);
        config.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);

        Assertions.assertThrows(IllegalStateException.class, config::tempoVersion);
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V2", "SCALING_SKILLS_V3"})
    void setterOrderDoesNotRejectAConsistentFinalConfiguration(SkillFormulaVersion version) {
        final var tempoFirst = new PlayerCombatConfig();
        tempoFirst.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);
        tempoFirst.setVersion(version);
        final var formulaFirst = new PlayerCombatConfig();
        formulaFirst.setVersion(version);
        formulaFirst.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);

        Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, tempoFirst.tempoVersion());
        Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, formulaFirst.tempoVersion());
    }

    @Test
    void changingFormulaAfterSelectingAcceleratedTempoIsRejectedWhenUsed() {
        final var config = new PlayerCombatConfig();
        config.setVersion(SkillFormulaVersion.SCALING_SKILLS_V2);
        config.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);
        config.setVersion(SkillFormulaVersion.LEGACY_SKILLS_V1);

        Assertions.assertThrows(IllegalStateException.class, config::tempoVersion);
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V2", "SCALING_SKILLS_V3"})
    void springBindsExplicitTempoWithoutDependingOnPropertyOrder(SkillFormulaVersion version) {
        final var tempoFirst = new LinkedHashMap<String, Object>();
        tempoFirst.put("homyakin.seeker.player-combat.tempo-version", "ACCELERATED_V1");
        tempoFirst.put("homyakin.seeker.player-combat.version", version.name());
        final var formulaFirst = new LinkedHashMap<String, Object>();
        formulaFirst.put("homyakin.seeker.player-combat.version", version.name());
        formulaFirst.put("homyakin.seeker.player-combat.tempo-version", "ACCELERATED_V1");

        for (final Map<String, Object> properties : List.of(tempoFirst, formulaFirst)) {
            final var config = new Binder(new MapConfigurationPropertySource(properties))
                .bind("homyakin.seeker.player-combat", Bindable.of(PlayerCombatConfig.class))
                .get();
            Assertions.assertEquals(version, config.version());
            Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, config.tempoVersion());
        }
    }
}
