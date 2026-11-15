package ru.homyakin.seeker.game.event.pve;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ru.homyakin.seeker.game.battle.BattlePersonage;

public record GeneratedPveEnemies(
    List<BattlePersonage> enemies,
    PveCharacteristicScaling.Result calibration,
    Optional<PveScalingDiagnostics> diagnostics
) {
    public GeneratedPveEnemies(List<BattlePersonage> enemies, PveCharacteristicScaling.Result calibration) {
        this(enemies, calibration, Optional.empty());
    }

    public GeneratedPveEnemies {
        enemies = List.copyOf(enemies);
        Objects.requireNonNull(calibration, "calibration");
        Objects.requireNonNull(diagnostics, "diagnostics");
    }
}
