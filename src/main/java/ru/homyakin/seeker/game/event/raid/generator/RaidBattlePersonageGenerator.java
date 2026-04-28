package ru.homyakin.seeker.game.event.raid.generator;

import java.util.List;

import ru.homyakin.seeker.game.battle.v3.two_team.BattlePersonage;

public interface RaidBattlePersonageGenerator {
    List<BattlePersonage> generate(int personagesCount, double powerPercent);
}
