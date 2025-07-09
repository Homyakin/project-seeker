package ru.homyakin.seeker.game.event.raid.generator;

import java.util.List;

import ru.homyakin.seeker.game.battle.v3.BattlePersonage;

public interface RaidBattlePersonageGenerator {
    /**
     * @param  personages список персонажей оппонентов
     * @param  powerPercent процент от 0 до 1 обозначающий итоговую мощь рейда
     */
    List<BattlePersonage> generate(List<BattlePersonage> personages, double powerPercent);
}
