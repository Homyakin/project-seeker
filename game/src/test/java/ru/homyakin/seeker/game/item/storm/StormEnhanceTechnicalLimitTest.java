package ru.homyakin.seeker.game.item.storm;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class StormEnhanceTechnicalLimitTest {
    private final StormEnhanceConfig config = new StormEnhanceConfig();
    private final StormEnhanceTechnicalLimit limit = new StormEnhanceTechnicalLimit(config);

    @Test
    void maxStateLevel_usesPriceBoundaryWhenItIsLower() {
        Assertions.assertEquals(config.maxPriceSupportedStateLevel(), limit.maxStateLevel(object(60)));
    }

    @Test
    void maxStateLevel_usesProgressionBoundaryWhenItIsLower() {
        Assertions.assertEquals(0, limit.maxStateLevel(object(Integer.MAX_VALUE)));
    }

    private static ItemObject object(int health) {
        return new ItemObject(
            "body",
            Set.of(PersonageSlot.BODY),
            Optional.empty(),
            Optional.empty(),
            health,
            0,
            0,
            0,
            0,
            0,
            Map.of()
        );
    }
}
