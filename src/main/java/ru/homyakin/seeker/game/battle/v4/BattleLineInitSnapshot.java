package ru.homyakin.seeker.game.battle.v4;

import java.util.List;
import java.util.UUID;

public record BattleLineInitSnapshot(
    int lineIndex,
    boolean firstTeam,
    Position position,
    List<UUID> personageIds
) {
}
