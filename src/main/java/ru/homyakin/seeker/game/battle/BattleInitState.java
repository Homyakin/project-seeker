package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BattleInitState(
    List<BattleLineInitSnapshot> lines,
    Map<UUID, BattlePersonageInitSnapshot> personagesById
) {
}
