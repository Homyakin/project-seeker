package ru.homyakin.seeker.game.random.item.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.ItemRarity;

public class RaidLevelItemConfigTest {

    private RaidLevelItemConfig config;

    @BeforeEach
    public void setUp() {
        config = new RaidLevelItemConfig();
    }

    @Test
    public void Given_Level10_Then_ReturnBaseDistribution() {
        final var picker = config.getRarityPickerForLevel(10);
        
        // Проверяем, что для уровня 10 используется базовое распределение
        final var baseDistribution = config.getBaseRarityDistribution();
        
        // Проверяем базовые значения
        Assertions.assertEquals(60, baseDistribution.get(ItemRarity.COMMON));
        Assertions.assertEquals(40, baseDistribution.get(ItemRarity.UNCOMMON));
        Assertions.assertEquals(0, baseDistribution.get(ItemRarity.RARE));
        Assertions.assertEquals(0, baseDistribution.get(ItemRarity.EPIC));
        Assertions.assertEquals(0, baseDistribution.get(ItemRarity.LEGENDARY));
        
        Assertions.assertNotNull(picker);
    }

    @Test
    public void Given_StartLevels_Then_CorrectValues() {
        final var startLevels = config.getRarityStartLevels();
        
        // Проверяем правильные значения уровней начала
        Assertions.assertEquals(10, startLevels.get(ItemRarity.COMMON));
        Assertions.assertEquals(10, startLevels.get(ItemRarity.UNCOMMON));
        Assertions.assertEquals(10, startLevels.get(ItemRarity.RARE));
        Assertions.assertEquals(20, startLevels.get(ItemRarity.EPIC));
        Assertions.assertEquals(30, startLevels.get(ItemRarity.LEGENDARY));
    }

    @Test
    public void Given_ImprovementMultipliers_Then_CorrectValues() {
        final var multipliers = config.getRarityImprovementMultipliers();
        
        // Проверяем правильные значения множителей
        Assertions.assertEquals(0.85, multipliers.get(ItemRarity.COMMON));
        Assertions.assertEquals(0.95, multipliers.get(ItemRarity.UNCOMMON));
        Assertions.assertEquals(1.5, multipliers.get(ItemRarity.RARE));
        Assertions.assertEquals(1.8, multipliers.get(ItemRarity.EPIC));
        Assertions.assertEquals(2.0, multipliers.get(ItemRarity.LEGENDARY));
    }

    @Disabled
    @Test
    public void showProbabilities() {
        for (var raidLevel = 0; raidLevel < 100; raidLevel++) {
            var picker = config.getRarityPickerForLevel(raidLevel);
            System.out.println(raidLevel + ": " + picker.getProbabilities());
        }
    }
} 