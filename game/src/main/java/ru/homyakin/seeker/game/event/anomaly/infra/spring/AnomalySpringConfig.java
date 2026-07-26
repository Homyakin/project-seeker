package ru.homyakin.seeker.game.event.anomaly.infra.spring;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.models.Money;

@ConfigurationProperties(prefix = "homyakin.seeker.event.anomaly")
public class AnomalySpringConfig implements AnomalyConfig {
    private int partySize;
    private Duration gatheringDuration;
    private Duration safePveDuration;
    private Duration dangerousSearchDuration;
    private Duration dangerousChallengeDuration;
    private Money safeReward;
    private Money victoryReward;
    private Money defeatReward;
    private Money noMatchReward;
    private int recentMeetPenaltyFirstDay = 256;
    private int initialRating;
    private int eloK;

    @Override
    public int partySize() {
        return partySize;
    }

    @Override
    public Duration gatheringDuration() {
        return gatheringDuration;
    }

    @Override
    public Duration safePveDuration() {
        return safePveDuration;
    }

    @Override
    public Duration dangerousSearchDuration() {
        return dangerousSearchDuration;
    }

    @Override
    public Duration dangerousChallengeDuration() {
        return dangerousChallengeDuration;
    }

    @Override
    public Money safeReward() {
        return safeReward;
    }

    @Override
    public Money victoryReward() {
        return victoryReward;
    }

    @Override
    public Money defeatReward() {
        return defeatReward;
    }

    @Override
    public Money noMatchReward() {
        return noMatchReward;
    }

    @Override
    public int recentMeetPenaltyFirstDay() {
        return recentMeetPenaltyFirstDay;
    }

    @Override
    public int initialRating() {
        return initialRating;
    }

    @Override
    public int eloK() {
        return eloK;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public void setGatheringDuration(Duration gatheringDuration) {
        this.gatheringDuration = gatheringDuration;
    }

    public void setSafePveDuration(Duration safePveDuration) {
        this.safePveDuration = safePveDuration;
    }

    public void setDangerousSearchDuration(Duration dangerousSearchDuration) {
        this.dangerousSearchDuration = dangerousSearchDuration;
    }

    public void setDangerousChallengeDuration(Duration dangerousChallengeDuration) {
        this.dangerousChallengeDuration = dangerousChallengeDuration;
    }

    public void setRewardSafe(int rewardSafe) {
        this.safeReward = Money.from(rewardSafe);
    }

    public void setRewardVictory(int rewardVictory) {
        this.victoryReward = Money.from(rewardVictory);
    }

    public void setRewardDefeat(int rewardDefeat) {
        this.defeatReward = Money.from(rewardDefeat);
    }

    public void setRewardNoMatch(int rewardNoMatch) {
        this.noMatchReward = Money.from(rewardNoMatch);
    }

    public void setRecentMeetPenaltyFirstDay(int recentMeetPenaltyFirstDay) {
        this.recentMeetPenaltyFirstDay = recentMeetPenaltyFirstDay;
    }

    public void setInitialRating(int initialRating) {
        this.initialRating = initialRating;
    }

    public void setEloK(int eloK) {
        this.eloK = eloK;
    }
}
