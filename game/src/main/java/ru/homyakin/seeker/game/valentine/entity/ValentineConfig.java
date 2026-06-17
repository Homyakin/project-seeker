package ru.homyakin.seeker.game.valentine.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import ru.homyakin.seeker.game.models.Money;

@ConfigurationProperties("homyakin.seeker.valentine")
public record ValentineConfig(
    int sameGroupGoldCost,
    int sameGroupEnergyCost,
    int otherGroupGoldCost,
    int otherGroupEnergyCost,
    int randomGroupGoldCost,
    int randomGroupEnergyCost,
    int badgeThreshold
) {
    public Money sameGroupGold() {
        return Money.from(sameGroupGoldCost);
    }

    public Money otherGroupGold() {
        return Money.from(otherGroupGoldCost);
    }

    public Money randomGroupGold() {
        return Money.from(randomGroupGoldCost);
    }
}
