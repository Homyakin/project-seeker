package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BattleInitState(
    List<BattleLineInitSnapshot> lines,
    Map<UUID, BattlePersonageInitSnapshot> personagesById,
    BattleTempoVersion tempoVersion
) {
    public BattleInitState {
        if (tempoVersion == null) {
            tempoVersion = BattleTempoVersion.BASELINE_V1;
        }
    }

    public BattleInitState(List<BattleLineInitSnapshot> lines, Map<UUID, BattlePersonageInitSnapshot> personagesById) {
        this(lines, personagesById, BattleTempoVersion.BASELINE_V1);
    }
}
