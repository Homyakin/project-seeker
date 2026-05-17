package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.v4.BattlePersonage;

import java.util.List;

public interface RaidBattlePersonageGenerator {
    List<BattlePersonage> generate(List<BattlePersonage> personages, double powerBonus);
}
