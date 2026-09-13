package ru.homyakin.seeker.game.item.storm;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.models.StormShards;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

@ConfigurationProperties(prefix = "homyakin.seeker.item.storm-enhance")
public class StormEnhanceConfig {
    private static volatile int activeBonusPercentPerLevel = 5;

    private int bonusPercentPerLevel = 5;
    private int baseCost = 10;
    private int costMultiplierNumerator = 5;
    private int costMultiplierDenominator = 4;

    private int baseSuccessPercent = 100;
    private double successMultiplier = 0.75;

    private int failurePeakLevel = 10;
    private double failureBaseWeight = 15;
    private double failureGrowMultiplier = 1.1;
    private double failureDecayMultiplier = 0.65;

    private int rollbackFromLevel = 4;
    private double rollbackBaseWeight = 4;
    private double rollbackGrowMultiplier = 1.25;

    @PostConstruct
    void activate() {
        validateCostConfiguration();
        activeBonusPercentPerLevel = bonusPercentPerLevel;
    }

    public int bonusPercentPerLevel() {
        return bonusPercentPerLevel;
    }

    /**
     * Applies configured storm-enhance bonus to a base item stat.
     */
    public int applyBonus(int base, int enhanceLevel) {
        return applyBonus(base, enhanceLevel, bonusPercentPerLevel());
    }

    /**
     * Applies the active Spring-bound bonus percent. Safe for domain models without DI.
     */
    public static int applyConfiguredBonus(int base, int enhanceLevel) {
        return applyBonus(base, enhanceLevel, activeBonusPercentPerLevel);
    }

    private static int applyBonus(int base, int enhanceLevel, int percentPerLevel) {
        if (enhanceLevel <= 0 || base == 0) {
            return base;
        }
        return (int) Math.round(base * (1 + enhanceLevel * percentPerLevel / 100.0));
    }

    public int failurePeakLevel() {
        return failurePeakLevel;
    }

    public int rollbackFromLevel() {
        return rollbackFromLevel;
    }

    public StormShards costForLevel(int currentLevel, Set<PersonageSlot> slots) {
        if (currentLevel < 0) {
            throw new IllegalArgumentException("Invalid enhance level: " + currentLevel);
        }
        final var maxStateLevel = maxPriceSupportedStateLevel();
        if (currentLevel >= maxStateLevel) {
            throw new ArithmeticException("Storm enhance price is not supported at level: " + currentLevel);
        }
        final var quarters = StormEnhanceSlotCoefficients.quarters(slots);
        final var fraction = priceFraction(currentLevel, quarters);
        final var rounded = roundHalfUp(fraction.numerator(), fraction.denominator());
        if (rounded.signum() < 1) {
            return StormShards.from(1);
        }
        return StormShards.from(rounded.intValueExact());
    }

    /**
     * The maximum state level for which every known non-empty slot mask has a representable price.
     * Attempts are allowed only below the returned level.
     */
    public int maxPriceSupportedStateLevel() {
        validateCostConfiguration();
        var fraction = priceFraction(0, StormEnhanceSlotCoefficients.maxQuarters());
        for (int level = 0; level < Integer.MAX_VALUE; level++) {
            if (!roundsToSupportedCost(fraction.numerator(), fraction.denominator())) {
                return level;
            }
            fraction = new PriceFraction(
                fraction.numerator().multiply(BigInteger.valueOf(costMultiplierNumerator)),
                fraction.denominator().multiply(BigInteger.valueOf(costMultiplierDenominator))
            );
        }
        return Integer.MAX_VALUE;
    }

    private PriceFraction priceFraction(int currentLevel, int slotQuarters) {
        final var numerator = BigInteger.valueOf(baseCost)
            .multiply(BigInteger.valueOf(slotQuarters))
            .multiply(BigInteger.valueOf(costMultiplierNumerator).pow(currentLevel));
        final var denominator = BigInteger.valueOf(4)
            .multiply(BigInteger.valueOf(costMultiplierDenominator).pow(currentLevel));
        return new PriceFraction(numerator, denominator);
    }

    private boolean roundsToSupportedCost(BigInteger numerator, BigInteger denominator) {
        final var maximumDoubledPlusOne = BigInteger.valueOf(Integer.MAX_VALUE).shiftLeft(1).add(BigInteger.ONE);
        return numerator.shiftLeft(1).compareTo(denominator.multiply(maximumDoubledPlusOne)) < 0;
    }

    private static BigInteger roundHalfUp(BigInteger numerator, BigInteger denominator) {
        final var quotientAndRemainder = numerator.divideAndRemainder(denominator);
        return quotientAndRemainder[1].shiftLeft(1).compareTo(denominator) >= 0
            ? quotientAndRemainder[0].add(BigInteger.ONE)
            : quotientAndRemainder[0];
    }

    private void validateCostConfiguration() {
        if (baseCost < 1) {
            throw new IllegalStateException("baseCost must be >= 1");
        }
        if (costMultiplierDenominator < 1) {
            throw new IllegalStateException("costMultiplierDenominator must be >= 1");
        }
        if (costMultiplierNumerator <= costMultiplierDenominator) {
            throw new IllegalStateException("cost multiplier must be greater than 1");
        }
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
        activeBonusPercentPerLevel = bonusPercentPerLevel;
    }

    public void setBaseCost(int baseCost) {
        if (baseCost < 1) {
            throw new IllegalStateException("baseCost must be >= 1");
        }
        this.baseCost = baseCost;
    }

    public void setCostMultiplierNumerator(int costMultiplierNumerator) {
        if (costMultiplierNumerator < 1) {
            throw new IllegalStateException("costMultiplierNumerator must be >= 1");
        }
        this.costMultiplierNumerator = costMultiplierNumerator;
    }

    public void setCostMultiplierDenominator(int costMultiplierDenominator) {
        if (costMultiplierDenominator < 1) {
            throw new IllegalStateException("costMultiplierDenominator must be >= 1");
        }
        this.costMultiplierDenominator = costMultiplierDenominator;
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

    private record PriceFraction(BigInteger numerator, BigInteger denominator) {
    }
}
