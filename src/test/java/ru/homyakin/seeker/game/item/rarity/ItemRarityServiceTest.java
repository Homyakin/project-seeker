package ru.homyakin.seeker.game.item.rarity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.utils.RandomUtils;

public class ItemRarityServiceTest {
    private final ItemRarityConfig config = new ItemRarityConfig();
    private final ItemRarityService service = new ItemRarityService(config);

    @BeforeEach
    public void init() {
        config.setCommonProbability(45);
        config.setUncommonProbability(25);
        config.setRareProbability(15);
        config.setEpicProbability(10);
        config.setLegendaryProbability(5);
    }

    @Test
    public void When_ProbabilityIsMaxLegendary_Then_RarityIsLegendary() {
        ItemRarity rarity;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(config.legendaryProbability());
            rarity = service.generateItemRarity();
        }

        Assertions.assertEquals(ItemRarity.LEGENDARY, rarity);
    }

    @Test
    public void When_ProbabilityIsLegendaryPlusEpic_Then_RarityIsEpic() {
        ItemRarity rarity;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(config.legendaryProbability() + config.epicProbability());
            rarity = service.generateItemRarity();
        }

        Assertions.assertEquals(ItemRarity.EPIC, rarity);
    }

    @Test
    public void When_ProbabilityIsLegendaryPlusEpicPlusRare_Then_RarityIsRare() {
        ItemRarity rarity;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(
                config.legendaryProbability() + config.epicProbability() + config.rareProbability()
            );
            rarity = service.generateItemRarity();
        }

        Assertions.assertEquals(ItemRarity.RARE, rarity);
    }

    @Test
    public void When_ProbabilityIsLegendaryPlusEpicPlusRarePlusUncommon_Then_RarityIsRare() {
        ItemRarity rarity;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(
                config.legendaryProbability() + config.epicProbability() + config.rareProbability() + config.uncommonProbability()
            );
            rarity = service.generateItemRarity();
        }

        Assertions.assertEquals(ItemRarity.UNCOMMON, rarity);
    }

    @Test
    public void When_ProbabilityIsLegendaryPlusEpicPlusRarePlusUncommonPlusCommon_Then_RarityIsRare() {
        ItemRarity rarity;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(
                config.legendaryProbability() + config.epicProbability() + config.rareProbability() + config.uncommonProbability() +
                    config.commonProbability()
            );
            rarity = service.generateItemRarity();
        }

        Assertions.assertEquals(ItemRarity.COMMON, rarity);
    }
}
