package ru.homyakin.seeker.game.event.anomaly.infra.spring;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;

@ConfigurationProperties(prefix = "homyakin.seeker.event.anomaly")
public class AnomalySpringConfig implements AnomalyConfig {
    private int partySize;
    private Duration gatheringDuration;
    private Duration safePveDuration;
    private Duration dangerousMinSearchDuration;
    private Duration dangerousSearchDuration;
    private RewardConfig reward = new RewardConfig();
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
    public Duration dangerousMinSearchDuration() {
        return dangerousMinSearchDuration;
    }

    @Override
    public Duration dangerousSearchDuration() {
        return dangerousSearchDuration;
    }

    @Override
    public AnomalyReward gvgWinReward() {
        return reward.gvgWin.toReward();
    }

    @Override
    public AnomalyReward gvgLoseReward() {
        return reward.gvgLose.toReward();
    }

    @Override
    public AnomalyReward gvgFallbackWinReward() {
        return reward.gvgFallbackWin.toReward();
    }

    @Override
    public AnomalyReward gvgFallbackLoseReward() {
        return reward.gvgFallbackLose.toReward();
    }

    @Override
    public AnomalyReward pveWinReward() {
        return reward.pveWin.toReward();
    }

    @Override
    public AnomalyReward pveLoseReward() {
        return reward.pveLose.toReward();
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

    public void setDangerousMinSearchDuration(Duration dangerousMinSearchDuration) {
        this.dangerousMinSearchDuration = dangerousMinSearchDuration;
    }

    public void setDangerousSearchDuration(Duration dangerousSearchDuration) {
        this.dangerousSearchDuration = dangerousSearchDuration;
    }

    public void setReward(RewardConfig reward) {
        this.reward = reward;
    }

    public RewardConfig getReward() {
        return reward;
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

    public static class RewardConfig {
        private OutcomeConfig gvgWin = new OutcomeConfig();
        private OutcomeConfig gvgLose = new OutcomeConfig();
        private OutcomeConfig gvgFallbackWin = new OutcomeConfig();
        private OutcomeConfig gvgFallbackLose = new OutcomeConfig();
        private OutcomeConfig pveWin = new OutcomeConfig();
        private OutcomeConfig pveLose = new OutcomeConfig();

        public OutcomeConfig getGvgWin() {
            return gvgWin;
        }

        public void setGvgWin(OutcomeConfig gvgWin) {
            this.gvgWin = gvgWin;
        }

        public OutcomeConfig getGvgLose() {
            return gvgLose;
        }

        public void setGvgLose(OutcomeConfig gvgLose) {
            this.gvgLose = gvgLose;
        }

        public OutcomeConfig getGvgFallbackWin() {
            return gvgFallbackWin;
        }

        public void setGvgFallbackWin(OutcomeConfig gvgFallbackWin) {
            this.gvgFallbackWin = gvgFallbackWin;
        }

        public OutcomeConfig getGvgFallbackLose() {
            return gvgFallbackLose;
        }

        public void setGvgFallbackLose(OutcomeConfig gvgFallbackLose) {
            this.gvgFallbackLose = gvgFallbackLose;
        }

        public OutcomeConfig getPveWin() {
            return pveWin;
        }

        public void setPveWin(OutcomeConfig pveWin) {
            this.pveWin = pveWin;
        }

        public OutcomeConfig getPveLose() {
            return pveLose;
        }

        public void setPveLose(OutcomeConfig pveLose) {
            this.pveLose = pveLose;
        }
    }

    public static class OutcomeConfig {
        private int money;
        private int stormShards;

        public AnomalyReward toReward() {
            return AnomalyReward.of(money, stormShards);
        }

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }

        public int getStormShards() {
            return stormShards;
        }

        public void setStormShards(int stormShards) {
            this.stormShards = stormShards;
        }
    }
}
