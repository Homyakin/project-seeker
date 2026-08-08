package ru.homyakin.seeker.game.event.anomaly.entity;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.models.StormShards;

public record AnomalyReward(
    Money money,
    StormShards stormShards
) {
    public static AnomalyReward of(int money, int stormShards) {
        return new AnomalyReward(Money.from(money), StormShards.from(stormShards));
    }
}
