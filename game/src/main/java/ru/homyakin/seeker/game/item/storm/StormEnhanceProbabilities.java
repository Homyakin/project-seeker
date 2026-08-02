package ru.homyakin.seeker.game.item.storm;

/**
 * Absolute percents for storm enhance outcomes. Always sums to 100.
 */
public record StormEnhanceProbabilities(
    int successPercent,
    int failurePercent,
    int rollbackPercent
) {
    public StormEnhanceProbabilities {
        if (successPercent < 0 || failurePercent < 0 || rollbackPercent < 0) {
            throw new IllegalArgumentException("Percents must be non-negative");
        }
        if (successPercent + failurePercent + rollbackPercent != 100) {
            throw new IllegalArgumentException(
                "Percents must sum to 100, got %d+%d+%d".formatted(
                    successPercent, failurePercent, rollbackPercent
                )
            );
        }
    }
}
