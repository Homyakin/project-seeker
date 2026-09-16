package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.utils.RandomUtils;

final class DefaultBattleRandom implements BattleRandom {
    @Override
    public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
        return RandomUtils.getInInterval(minimumInclusive, maximumInclusive);
    }
}
