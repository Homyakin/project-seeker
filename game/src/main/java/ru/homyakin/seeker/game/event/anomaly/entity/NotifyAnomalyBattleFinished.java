package ru.homyakin.seeker.game.event.anomaly.entity;

import ru.homyakin.seeker.game.event.models.EventResult;

public interface NotifyAnomalyBattleFinished {
    void notify(EventResult.AnomalyResult.BattleFinished result);
}
