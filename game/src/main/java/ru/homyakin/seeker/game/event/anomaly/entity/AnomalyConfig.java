package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.Duration;

public interface AnomalyConfig {
    int partySize();

    Duration gatheringDuration();

    Duration safePveDuration();

    /**
     * Minimum time in SEARCHING before a pool match is allowed.
     */
    Duration dangerousMinSearchDuration();

    /**
     * Maximum SEARCHING duration; then PvE fallback.
     */
    Duration dangerousSearchDuration();

    AnomalyReward gvgWinReward();

    AnomalyReward gvgLoseReward();

    AnomalyReward gvgFallbackWinReward();

    AnomalyReward gvgFallbackLoseReward();

    AnomalyReward pveWinReward();

    AnomalyReward pveLoseReward();

    /**
     * Score penalty for a fight within the last day; halves each next day until 0.
     */
    int recentMeetPenaltyFirstDay();

    int initialRating();

    int eloK();
}
