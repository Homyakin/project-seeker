package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

public record PersonageStats(
    SeasonNumber seasonNumber,
    PersonageId personageId,
    int raidsSuccess,
    int raidsTotal,
    int duelsWins,
    int duelsTotal,
    long tavernMoneySpent,
    int spinWinsCount,
    int questsSuccess,
    int questsTotal,
    int worldRaidsSuccess,
    int worldRaidsTotal
) {
}
