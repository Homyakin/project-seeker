package ru.homyakin.seeker.game.item.modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.GenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.item.rarity.ItemRarityConfig;
import ru.homyakin.seeker.game.item.rarity.ItemRarityService;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.List;
import java.util.Map;

public class ItemModifierServiceTest {
    private final ItemModifierConfig config = new ItemModifierConfig();
    private final ItemModifierDao dao = Mockito.mock(ItemModifierDao.class);
    private final ItemModifierService service = new ItemModifierService(dao, config);

    @BeforeEach
    public void init() {
        config.setZeroProbability(60);
        config.setOneProbability(30);
        config.setTwoProbability(10);
    }

    @Test
    public void When_ProbabilityIsMaxZero_Then_ReturnEmptyList() {
        List<GenerateModifier> modifiers;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(config.zeroProbability());
            modifiers = service.generateModifiersForRarity(ItemRarity.COMMON);
        }

        Assertions.assertEquals(0, modifiers.size());
    }

    @Test
    public void When_ProbabilityIsZeroPlusOne_Then_ReturnOneModifier() {
        Mockito.when(dao.getRandomModifier(ItemRarity.COMMON)).thenReturn(prefixModifier);
        List<GenerateModifier> modifiers;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(config.zeroProbability() + config.oneProbability());
            modifiers = service.generateModifiersForRarity(ItemRarity.COMMON);
        }

        Assertions.assertEquals(1, modifiers.size());
        Assertions.assertEquals(prefixModifier, modifiers.getFirst());}

    @Test
    public void When_ProbabilityIsZeroPlusOnePlusTwo_And_FirstIsPrefix_Then_ReturnTwoPrefixes() {
        Mockito.when(dao.getRandomModifier(ItemRarity.COMMON)).thenReturn(prefixModifier);
        Mockito.when(dao.getRandomModifierExcludeId(prefixModifier.id(), ItemRarity.COMMON)).thenReturn(prefixModifier);
        List<GenerateModifier> modifiers;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(
                config.zeroProbability() + config.oneProbability() + config.twoProbability()
            );
            modifiers = service.generateModifiersForRarity(ItemRarity.COMMON);
        }

        Assertions.assertEquals(2, modifiers.size());
        Assertions.assertEquals(prefixModifier, modifiers.get(0));
        Assertions.assertEquals(prefixModifier, modifiers.get(1));
    }

    @Test
    public void When_ProbabilityIsZeroPlusOnePlusTwo_And_FirstIsSuffix_Then_ReturnOnePrefixAndOneSuffix() {
        Mockito.when(dao.getRandomModifier(ItemRarity.COMMON)).thenReturn(suffixModifier);
        Mockito.when(dao.getRandomModifierWithType(ModifierType.PREFIX, ItemRarity.COMMON)).thenReturn(prefixModifier);
        List<GenerateModifier> modifiers;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.getInInterval(1, 100)).thenReturn(
                config.zeroProbability() + config.oneProbability() + config.twoProbability()
            );
            modifiers = service.generateModifiersForRarity(ItemRarity.COMMON);
        }

        Assertions.assertEquals(2, modifiers.size());
        Assertions.assertEquals(suffixModifier, modifiers.get(0));
        Assertions.assertEquals(prefixModifier, modifiers.get(1));
    }

    private final GenerateModifier prefixModifier = new GenerateModifier(
        1,
        "",
        ModifierType.PREFIX,
        Mockito.mock(ModifierGenerateCharacteristics.class),
        Map.of()
    );

    private final GenerateModifier suffixModifier = new GenerateModifier(
        1,
        "",
        ModifierType.SUFFIX,
        Mockito.mock(ModifierGenerateCharacteristics.class),
        Map.of()
    );
}
