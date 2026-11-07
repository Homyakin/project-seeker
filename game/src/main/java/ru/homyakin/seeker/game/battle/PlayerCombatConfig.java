package ru.homyakin.seeker.game.battle;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;

/** Explicit rules for newly created player battles, independent of their currently equipped items. */
@ConfigurationProperties(prefix = "homyakin.seeker.player-combat")
public class PlayerCombatConfig {
    private SkillFormulaVersion version = SkillFormulaVersion.LEGACY_SKILLS_V1;
    private BattleTempoVersion tempoVersion = BattleTempoVersion.BASELINE_V1;

    public SkillFormulaVersion version() {
        return version;
    }

    public void setVersion(SkillFormulaVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Player combat version must be specified");
        }
        this.version = version;
    }

    public BattleTempoVersion tempoVersion() {
        if (tempoVersion == BattleTempoVersion.ACCELERATED_V1 && !version.isV2OrLater()) {
            throw new IllegalStateException("Accelerated battle tempo requires player combat SCALING_SKILLS_V2 or SCALING_SKILLS_V3");
        }
        return tempoVersion;
    }

    public void setTempoVersion(BattleTempoVersion tempoVersion) {
        if (tempoVersion == null) {
            throw new IllegalArgumentException("Player battle tempo version must be specified");
        }
        this.tempoVersion = tempoVersion;
    }
}
