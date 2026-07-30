package ru.homyakin.seeker.game.item.storm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StormEnhanceConfigTest {

    @Test
    void Given_DefaultConfig_When_CostAndChance_Then_IncreaseCostAndDecreaseChance() {
        final var config = new StormEnhanceConfig();

        Assertions.assertEquals(1, config.costForLevel(0).value());
        Assertions.assertEquals(2, config.costForLevel(1).value());
        Assertions.assertEquals(3, config.costForLevel(2).value());

        Assertions.assertEquals(100, config.successPercentForLevel(0));
        Assertions.assertEquals(90, config.successPercentForLevel(1));
        Assertions.assertEquals(80, config.successPercentForLevel(2));
    }

    @Test
    void Given_MaxLevel_When_CostForLevel_Then_Throws() {
        final var config = new StormEnhanceConfig();
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.costForLevel(config.maxLevel()));
    }
}
