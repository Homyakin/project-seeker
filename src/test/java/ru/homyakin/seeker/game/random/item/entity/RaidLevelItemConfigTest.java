package ru.homyakin.seeker.game.random.item.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;

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
        Assertions.assertEquals(60, baseDistribution.get(LegacyItemRarity.COMMON));
        Assertions.assertEquals(40, baseDistribution.get(LegacyItemRarity.UNCOMMON));
        Assertions.assertEquals(0, baseDistribution.get(LegacyItemRarity.RARE));
        Assertions.assertEquals(0, baseDistribution.get(LegacyItemRarity.EPIC));
        Assertions.assertEquals(0, baseDistribution.get(LegacyItemRarity.LEGENDARY));
        
        Assertions.assertNotNull(picker);
    }

    @Test
    public void Given_StartLevels_Then_CorrectValues() {
        final var startLevels = config.getRarityStartLevels();
        
        // Проверяем правильные значения уровней начала
        Assertions.assertEquals(10, startLevels.get(LegacyItemRarity.COMMON));
        Assertions.assertEquals(10, startLevels.get(LegacyItemRarity.UNCOMMON));
        Assertions.assertEquals(10, startLevels.get(LegacyItemRarity.RARE));
        Assertions.assertEquals(20, startLevels.get(LegacyItemRarity.EPIC));
        Assertions.assertEquals(30, startLevels.get(LegacyItemRarity.LEGENDARY));
    }

    @Test
    public void Given_ImprovementMultipliers_Then_CorrectValues() {
        final var multipliers = config.getRarityImprovementMultipliers();
        
        // Проверяем правильные значения множителей
        Assertions.assertEquals(0.85, multipliers.get(LegacyItemRarity.COMMON));
        Assertions.assertEquals(0.95, multipliers.get(LegacyItemRarity.UNCOMMON));
        Assertions.assertEquals(1.5, multipliers.get(LegacyItemRarity.RARE));
        Assertions.assertEquals(1.8, multipliers.get(LegacyItemRarity.EPIC));
        Assertions.assertEquals(2.0, multipliers.get(LegacyItemRarity.LEGENDARY));
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