package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.Duration;
import ru.homyakin.seeker.game.models.Money;

public interface AnomalyConfig {
    int partySize();

    Duration gatheringDuration();

    Duration dangerousSearchDuration();

    Money safeReward();

    Money victoryReward();

    Money defeatReward();

    Money noMatchReward();

    int initialRatingDelta();

    int ratingDeltaExpandPerHour();

    Duration recentOpponentPenaltyHours();

    int initialRating();

    int eloK();
}
