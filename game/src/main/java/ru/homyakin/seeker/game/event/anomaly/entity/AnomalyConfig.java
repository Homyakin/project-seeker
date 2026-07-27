package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.Duration;
import ru.homyakin.seeker.game.models.Money;

public interface AnomalyConfig {
    int partySize();

    Duration gatheringDuration();

    Duration safePveDuration();

    Duration dangerousSearchDuration();

    Duration dangerousChallengeDuration();

    Money safeReward();

    Money victoryReward();

    Money defeatReward();

    /**
     * Score penalty for a fight within the last day; halves each next day until 0.
     */
    int recentMeetPenaltyFirstDay();

    int initialRating();

    int eloK();
}
