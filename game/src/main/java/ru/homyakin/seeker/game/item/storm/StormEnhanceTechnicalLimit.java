package ru.homyakin.seeker.game.item.storm;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.ItemObject;

/**
 * Calculates the highest representable enhancement state for an item.
 */
@Component
public class StormEnhanceTechnicalLimit {
    private final StormEnhanceConfig config;

    public StormEnhanceTechnicalLimit(StormEnhanceConfig config) {
        this.config = config;
    }

    public int maxStateLevel(ItemObject object) {
        return Math.min(
            config.maxPriceSupportedStateLevel(),
            ItemProgression.maxSupportedLevel(object)
        );
    }
}
