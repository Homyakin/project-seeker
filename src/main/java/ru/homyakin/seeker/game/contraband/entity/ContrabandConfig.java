package ru.homyakin.seeker.game.contraband.entity;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.utils.ProbabilityPicker;
import ru.homyakin.seeker.utils.models.IntRange;

@ConfigurationProperties("homyakin.seeker.contraband")
public class ContrabandConfig {
    private int commonMaxRaidLevel = 15;
    private int uncommonMaxRaidLevel = 30;
    private int rareMaxRaidLevel = 45;
    private int dropChancePercent = 20;
    private int finderSuccessChancePercent = 30;
    private int finderChanceIncreasePerFail = 15;
    private int receiverSuccessChancePercent = 70;
    private int receiverChanceIncreasePerFail = 15;
    private int expirationHours = 12;
    private Duration receiverActivityDuration = Duration.ofHours(24);

    private int commonSellPrice = 10;
    private int uncommonSellPrice = 50;
    private int rareSellPrice = 100;
    private int epicSellPrice = 200;

    private Map<ContrabandTier, ProbabilityPicker<ContrabandReward>> rewards = Map.of(
        ContrabandTier.COMMON, new ProbabilityPicker<>(Map.of(
            new ContrabandReward.Gold(new IntRange(20, 40)), 50,
            new ContrabandReward.Item(new ProbabilityPicker<>(Map.of(
                ItemRarity.COMMON, 30,
                ItemRarity.UNCOMMON, 70
            ))), 30,
            new ContrabandReward.HealthBuff(IntRange.of(5, 10), IntRange.of(5, 10)), 10,
            new ContrabandReward.AttackBuff(IntRange.of(5, 10), IntRange.of(5, 10)), 10
        )),
        ContrabandTier.UNCOMMON, new ProbabilityPicker<>(Map.of(
            new ContrabandReward.Gold(new IntRange(70, 140)), 40,
            new ContrabandReward.Energy(new IntRange(15, 25)), 15,
            new ContrabandReward.Item(new ProbabilityPicker<>(Map.of(
                ItemRarity.COMMON, 10,
                ItemRarity.UNCOMMON, 60,
                ItemRarity.RARE, 30
            ))), 30,
            new ContrabandReward.HealthBuff(IntRange.of(7, 12), IntRange.of(5, 10)), 5,
            new ContrabandReward.AttackBuff(IntRange.of(7, 12), IntRange.of(5, 10)), 5
        )),
        ContrabandTier.RARE, new ProbabilityPicker<>(Map.of(
            new ContrabandReward.Gold(new IntRange(150, 250)), 30,
            new ContrabandReward.Energy(new IntRange(25, 35)), 15,
            new ContrabandReward.Item(new ProbabilityPicker<>(Map.of(
                ItemRarity.UNCOMMON, 15,
                ItemRarity.RARE, 60,
                ItemRarity.EPIC, 35
            ))), 30,
            new ContrabandReward.HealthBuff(IntRange.of(10, 15), IntRange.of(5, 10)), 5,
            new ContrabandReward.AttackBuff(IntRange.of(10, 15), IntRange.of(5, 10)), 5
        )),
        ContrabandTier.EPIC, new ProbabilityPicker<>(Map.of(
            new ContrabandReward.Gold(new IntRange(300, 400)), 40,
            new ContrabandReward.Energy(new IntRange(30, 50)), 20,
            new ContrabandReward.Item(new ProbabilityPicker<>(Map.of(
                ItemRarity.RARE, 15,
                ItemRarity.EPIC, 60,
                ItemRarity.LEGENDARY, 35
            ))), 40
        ))
    );

    private Map<ContrabandTier, ProbabilityPicker<ContrabandPenalty>> penalties = Map.of(
        ContrabandTier.COMMON, new ProbabilityPicker<>(Map.of(
            new ContrabandPenalty.HealthDebuff(IntRange.of(10, 15), IntRange.of(3, 6)), 40,
            new ContrabandPenalty.AttackDebuff(IntRange.of(10, 15), IntRange.of(3, 6)), 40,
            ContrabandPenalty.Nothing.INSTANCE, 20
        )),
        ContrabandTier.UNCOMMON, new ProbabilityPicker<>(Map.of(
            new ContrabandPenalty.HealthDebuff(IntRange.of(12, 17), IntRange.of(3, 6)), 35,
            new ContrabandPenalty.AttackDebuff(IntRange.of(12, 17), IntRange.of(3, 6)), 35,
            new ContrabandPenalty.GoldLoss(new IntRange(30, 60)), 15,
            ContrabandPenalty.Nothing.INSTANCE, 15
        )),
        ContrabandTier.RARE, new ProbabilityPicker<>(Map.of(
            new ContrabandPenalty.HealthDebuff(IntRange.of(15, 20), IntRange.of(3, 6)), 30,
            new ContrabandPenalty.AttackDebuff(IntRange.of(15, 20), IntRange.of(3, 6)), 30,
            new ContrabandPenalty.GoldLoss(new IntRange(75, 100)), 30,
            ContrabandPenalty.Nothing.INSTANCE, 10
        )),
        ContrabandTier.EPIC, new ProbabilityPicker<>(Map.of(
            new ContrabandPenalty.HealthDebuff(IntRange.of(25, 30), IntRange.of(3, 6)), 30,
            new ContrabandPenalty.AttackDebuff(IntRange.of(25, 30), IntRange.of(3, 6)), 30,
            new ContrabandPenalty.GoldLoss(new IntRange(100, 200)), 30
        ))
    );

    public ProbabilityPicker<ContrabandReward> rewards(ContrabandTier tier) {
        return rewards.get(tier);
    }

    public void setRewards(Map<ContrabandTier, ProbabilityPicker<ContrabandReward>> rewards) {
        this.rewards = rewards;
    }

    public ProbabilityPicker<ContrabandPenalty> penalties(ContrabandTier tier) {
        return penalties.get(tier);
    }

    public void setPenalties(Map<ContrabandTier, ProbabilityPicker<ContrabandPenalty>> penalties) {
        this.penalties = penalties;
    }

    public ContrabandTier tierForRaidLevel(int raidLevel) {
        if (raidLevel <= commonMaxRaidLevel) {
            return ContrabandTier.COMMON;
        } else if (raidLevel <= uncommonMaxRaidLevel) {
            return ContrabandTier.UNCOMMON;
        } else if (raidLevel <= rareMaxRaidLevel) {
            return ContrabandTier.RARE;
        } else {
            return ContrabandTier.EPIC;
        }
    }

    public Money sellPrice(ContrabandTier tier) {
        return new Money(switch (tier) {
            case COMMON -> commonSellPrice;
            case UNCOMMON -> uncommonSellPrice;
            case RARE -> rareSellPrice;
            case EPIC -> epicSellPrice;
        });
    }

    public int dropChancePercent() {
        return dropChancePercent;
    }

    public int finderSuccessChancePercent() {
        return finderSuccessChancePercent;
    }

    public int finderChanceIncreasePerFail() {
        return finderChanceIncreasePerFail;
    }

    public int receiverSuccessChancePercent() {
        return receiverSuccessChancePercent;
    }

    public int receiverChanceIncreasePerFail() {
        return receiverChanceIncreasePerFail;
    }

    public int expirationHours() {
        return expirationHours;
    }

    public Duration receiverActivityDuration() {
        return receiverActivityDuration;
    }

    public void setCommonMaxRaidLevel(int commonMaxRaidLevel) {
        this.commonMaxRaidLevel = commonMaxRaidLevel;
    }

    public void setUncommonMaxRaidLevel(int uncommonMaxRaidLevel) {
        this.uncommonMaxRaidLevel = uncommonMaxRaidLevel;
    }

    public void setRareMaxRaidLevel(int rareMaxRaidLevel) {
        this.rareMaxRaidLevel = rareMaxRaidLevel;
    }

    public void setDropChancePercent(int dropChancePercent) {
        this.dropChancePercent = dropChancePercent;
    }

    public void setFinderSuccessChancePercent(int v) {
        this.finderSuccessChancePercent = v;
    }

    public void setFinderChanceIncreasePerFail(int v) {
        this.finderChanceIncreasePerFail = v;
    }

    public void setReceiverSuccessChancePercent(int v) {
        this.receiverSuccessChancePercent = v;
    }

    public void setReceiverChanceIncreasePerFail(int v) {
        this.receiverChanceIncreasePerFail = v;
    }

    public void setExpirationHours(int expirationHours) {
        this.expirationHours = expirationHours;
    }

    public void setReceiverActivityDuration(Duration receiverActivityDuration) {
        this.receiverActivityDuration = receiverActivityDuration;
    }

    public void setCommonSellPrice(int v) {
        this.commonSellPrice = v;
    }

    public void setUncommonSellPrice(int v) {
        this.uncommonSellPrice = v;
    }

    public void setRareSellPrice(int v) {
        this.rareSellPrice = v;
    }

    public void setEpicSellPrice(int v) {
        this.epicSellPrice = v;
    }
}
