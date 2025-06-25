package ru.homyakin.seeker.game.event.world_raid.infra.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.models.Money;

import java.time.Duration;

@ConfigurationProperties(prefix = "homyakin.seeker.event.world-raid")
public class WorldRaidSpringConfig implements WorldRaidConfig {
    private static final Money INIT_FUND = Money.from(500);
    private Money requiredForDonate;
    private int averageRequiredContribution;
    private Money fundFromDonation;
    private Money fundFromQuest;
    private Duration battleDuration;
    private int requiredEnergy;
    private Duration groupNotificationInterval;

    @Override
    public Money requiredForDonate() {
        return requiredForDonate;
    }

    @Override
    public int averageRequiredContribution() {
        return averageRequiredContribution;
    }

    @Override
    public Money fundFromDonation() {
        return fundFromDonation;
    }

    @Override
    public Money fundFromQuest() {
        return fundFromQuest;
    }

    @Override
    public Duration battleDuration() {
        return battleDuration;
    }

    @Override
    public int requiredEnergy() {
        return requiredEnergy;
    }

    @Override
    public Duration groupNotificationInterval() {
        return groupNotificationInterval;
    }

    @Override
    public Money initFund() {
        return INIT_FUND;
    }

    public void setRequiredForDonate(int requiredForDonate) {
        this.requiredForDonate = Money.from(requiredForDonate);
    }

    public void setAverageRequiredContribution(int averageRequiredContribution) {
        this.averageRequiredContribution = averageRequiredContribution;
    }

    public void setFundFromDonation(int fundFromDonation) {
        this.fundFromDonation = Money.from(fundFromDonation);
    }

    public void setFundFromQuest(int fundFromQuest) {
        this.fundFromQuest = Money.from(fundFromQuest);
    }

    public void setBattleDuration(Duration battleDuration) {
        this.battleDuration = battleDuration;
    }

    public void setRequiredEnergy(int requiredEnergy) {
        this.requiredEnergy = requiredEnergy;
    }

    public void setGroupNotificationInterval(Duration groupNotificationInterval) {
        this.groupNotificationInterval = groupNotificationInterval;
    }
}
