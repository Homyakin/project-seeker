package ru.homyakin.seeker.game.item.storm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.models.StormShards;

@ConfigurationProperties(prefix = "homyakin.seeker.item.storm-enhance")
public class StormEnhanceConfig {
    private int bonusPercentPerLevel = 5;
    private int maxLevel = 10;
    private int baseCost = 1;
    private int costPerLevel = 1;
    private int baseSuccessPercent = 100;
    private int successPercentDecay = 10;

    public int bonusPercentPerLevel() {
        return bonusPercentPerLevel;
    }

    public int maxLevel() {
        return maxLevel;
    }

    /**
     * Cost to enhance from {@code currentLevel} to {@code currentLevel + 1}.
     */
    public StormShards costForLevel(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= maxLevel) {
            throw new IllegalArgumentException("Invalid enhance level: " + currentLevel);
        }
        return StormShards.from(baseCost + currentLevel * costPerLevel);
    }

    /**
     * Success chance (%) to enhance from {@code currentLevel} to {@code currentLevel + 1}.
     */
    public int successPercentForLevel(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= maxLevel) {
            throw new IllegalArgumentException("Invalid enhance level: " + currentLevel);
        }
        return Math.max(0, baseSuccessPercent - currentLevel * successPercentDecay);
    }

    public void setBonusPercentPerLevel(int bonusPercentPerLevel) {
        if (bonusPercentPerLevel < 0) {
            throw new IllegalStateException("bonusPercentPerLevel must be >= 0");
        }
        this.bonusPercentPerLevel = bonusPercentPerLevel;
    }

    public void setMaxLevel(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalStateException("maxLevel must be >= 1");
        }
        this.maxLevel = maxLevel;
    }

    public void setBaseCost(int baseCost) {
        if (baseCost < 1) {
            throw new IllegalStateException("baseCost must be >= 1");
        }
        this.baseCost = baseCost;
    }

    public void setCostPerLevel(int costPerLevel) {
        if (costPerLevel < 0) {
            throw new IllegalStateException("costPerLevel must be >= 0");
        }
        this.costPerLevel = costPerLevel;
    }

    public void setBaseSuccessPercent(int baseSuccessPercent) {
        if (baseSuccessPercent < 0 || baseSuccessPercent > 100) {
            throw new IllegalStateException("baseSuccessPercent must be in 0..100");
        }
        this.baseSuccessPercent = baseSuccessPercent;
    }

    public void setSuccessPercentDecay(int successPercentDecay) {
        if (successPercentDecay < 0) {
            throw new IllegalStateException("successPercentDecay must be >= 0");
        }
        this.successPercentDecay = successPercentDecay;
    }
}
