package ru.homyakin.seeker.game.item.storm;

import java.util.Map;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.models.StormEnhanceOutcome;
import ru.homyakin.seeker.utils.ProbabilityPicker;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class StormEnhanceOutcomePicker {
    public StormEnhanceOutcome pick(StormEnhanceProbabilities probabilities) {
        return new ProbabilityPicker<>(Map.of(
            StormEnhanceOutcome.SUCCESS, probabilities.successPercent(),
            StormEnhanceOutcome.FAILURE, probabilities.failurePercent(),
            StormEnhanceOutcome.ROLLBACK, probabilities.rollbackPercent()
        )).pick(RandomUtils::getWithMax);
    }
}
