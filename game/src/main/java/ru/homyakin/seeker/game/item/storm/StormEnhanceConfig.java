package ru.homyakin.seeker.game.item.storm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.models.StormShards;

@ConfigurationProperties(prefix = "homyakin.seeker.item.storm-enhance")
public class StormEnhanceConfig {
    private int bonusPercentPerLevel = 5;
    private int baseCost = 1;
    private double costMultiplier = 2.0;

    private int baseSuccessPercent = 100;
    private double successMultiplier = 0.75;

    private int failurePeakLevel = 10;
    private double failureBaseWeight = 15;
    private double failureGrowMultiplier = 1.1;
    private double failureDecayMultiplier = 0.65;

    private int rollbackFromLevel = 4;
    private double rollbackBaseWeight = 4;
    private double rollbackGrowMultiplier = 1.25;

    public int bonusPercentPerLevel() {
        return bonusPercentPerLevel;
    }

    public int failurePeakLevel() {
        return failurePeakLevel;
    }

    public int rollbackFromLevel() {
        return rollbackFromLevel;
    }

    /**
     * Cost to enhance from {@code currentLevel} to {@code currentLevel + 1}.
     * Grows exponentially: {@code baseCost * costMultiplier^currentLevel * slots}.
     */
    public StormShards costForLevel(int currentLevel, int slots) {
        if (currentLevel < 0) {
            throw new IllegalArgumentException("Invalid enhance level: " + currentLevel);
        }
        if (slots < 1) {
            throw new IllegalArgumentException("Invalid slot count: " + slots);
        }
        final var cost = baseCost * Math.pow(costMultiplier, currentLevel) * slots;
        if (Double.isNaN(cost) || Double.isInfinite(cost) || cost >= Integer.MAX_VALUE) {
            return StormShards.from(Integer.MAX_VALUE);
        }
        return StormShards.from((int) Math.max(1, Math.round(cost)));
    }

    /**
     * Outcome weights for enhancing from {@code currentLevel}. Sum is always 100.
     * <ul>
     *   <li>success — always decreases</li>
     *   <li>failure — grows until {@link #failurePeakLevel}, then decreases</li>
     *   <li>rollback — zero before {@link #rollbackFromLevel}, then grows</li>
     * </ul>
     */
    public StormEnhanceProbabilities probabilitiesForLevel(int currentLevel) {
        if (currentLevel < 0) {
            throw new IllegalArgumentException("Invalid enhance level: " + currentLevel);
        }
        final var successWeight = successWeight(currentLevel);
        final var failureWeight = failureWeight(currentLevel);
        final var rollbackWeight = rollbackWeight(currentLevel);
        final var total = successWeight + failureWeight + rollbackWeight;
        if (total <= 0) {
            return new StormEnhanceProbabilities(100, 0, 0);
        }

        var success = (int) Math.round(100.0 * successWeight / total);
        var failure = (int) Math.round(100.0 * failureWeight / total);
        var rollback = 100 - success - failure;
        if (rollback < 0) {
            final var over = -rollback;
            final var fromFailure = Math.min(failure, over);
            failure -= fromFailure;
            success -= over - fromFailure;
            rollback = 0;
        }
        if (success < 1) {
            final var need = 1 - success;
            success = 1;
            if (failure >= need) {
                failure -= need;
            } else {
                final var fromFailure = failure;
                failure = 0;
                rollback = Math.max(0, rollback - (need - fromFailure));
            }
            rollback = 100 - success - failure;
        }
        return new StormEnhanceProbabilities(success, failure, rollback);
    }

    private double successWeight(int level) {
        return baseSuccessPercent * Math.pow(successMultiplier, level);
    }

    private double failureWeight(int level) {
        if (level == 0) {
            return 0;
        }
        if (level <= failurePeakLevel) {
            return failureBaseWeight * Math.pow(failureGrowMultiplier, level - 1);
        }
        return failureWeight(failurePeakLevel) * Math.pow(failureDecayMultiplier, level - failurePeakLevel);
    }

    private double rollbackWeight(int level) {
        if (level < rollbackFromLevel) {
            return 0;
        }
        return rollbackBaseWeight * Math.pow(rollbackGrowMultiplier, level - rollbackFromLevel);
    }

    public void setBonusPercentPerLevel(int bonusPercentPerLevel) {
        if (bonusPercentPerLevel < 0) {
            throw new IllegalStateException("bonusPercentPerLevel must be >= 0");
        }
        this.bonusPercentPerLevel = bonusPercentPerLevel;
    }

    public void setBaseCost(int baseCost) {
        if (baseCost < 1) {
            throw new IllegalStateException("baseCost must be >= 1");
        }
        this.baseCost = baseCost;
    }

    public void setCostMultiplier(double costMultiplier) {
        if (costMultiplier < 1.0) {
            throw new IllegalStateException("costMultiplier must be >= 1");
        }
        this.costMultiplier = costMultiplier;
    }

    public void setBaseSuccessPercent(int baseSuccessPercent) {
        if (baseSuccessPercent < 0 || baseSuccessPercent > 100) {
            throw new IllegalStateException("baseSuccessPercent must be in 0..100");
        }
        this.baseSuccessPercent = baseSuccessPercent;
    }

    public void setSuccessMultiplier(double successMultiplier) {
        if (successMultiplier <= 0.0 || successMultiplier > 1.0) {
            throw new IllegalStateException("successMultiplier must be in (0, 1]");
        }
        this.successMultiplier = successMultiplier;
    }

    public void setFailurePeakLevel(int failurePeakLevel) {
        if (failurePeakLevel < 1) {
            throw new IllegalStateException("failurePeakLevel must be >= 1");
        }
        this.failurePeakLevel = failurePeakLevel;
    }

    public void setFailureBaseWeight(double failureBaseWeight) {
        if (failureBaseWeight < 0) {
            throw new IllegalStateException("failureBaseWeight must be >= 0");
        }
        this.failureBaseWeight = failureBaseWeight;
    }

    public void setFailureGrowMultiplier(double failureGrowMultiplier) {
        if (failureGrowMultiplier < 1.0) {
            throw new IllegalStateException("failureGrowMultiplier must be >= 1");
        }
        this.failureGrowMultiplier = failureGrowMultiplier;
    }

    public void setFailureDecayMultiplier(double failureDecayMultiplier) {
        if (failureDecayMultiplier <= 0.0 || failureDecayMultiplier > 1.0) {
            throw new IllegalStateException("failureDecayMultiplier must be in (0, 1]");
        }
        this.failureDecayMultiplier = failureDecayMultiplier;
    }

    public void setRollbackFromLevel(int rollbackFromLevel) {
        if (rollbackFromLevel < 0) {
            throw new IllegalStateException("rollbackFromLevel must be >= 0");
        }
        this.rollbackFromLevel = rollbackFromLevel;
    }

    public void setRollbackBaseWeight(double rollbackBaseWeight) {
        if (rollbackBaseWeight < 0) {
            throw new IllegalStateException("rollbackBaseWeight must be >= 0");
        }
        this.rollbackBaseWeight = rollbackBaseWeight;
    }

    public void setRollbackGrowMultiplier(double rollbackGrowMultiplier) {
        if (rollbackGrowMultiplier < 1.0) {
            throw new IllegalStateException("rollbackGrowMultiplier must be >= 1");
        }
        this.rollbackGrowMultiplier = rollbackGrowMultiplier;
    }
}
