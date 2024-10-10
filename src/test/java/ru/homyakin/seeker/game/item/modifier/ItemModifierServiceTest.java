package ru.homyakin.seeker.game.item.modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.GenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
import ru.homyakin.seeker.game.item.models.ItemRarity;

import java.util.Map;

public class ItemModifierServiceTest {
    private final ItemModifierDao dao = Mockito.mock(ItemModifierDao.class);
    private final ItemModifierService service = new ItemModifierService(dao);

    @Test
    public void When_GenerateZeroModifiers_Then_ReturnEmptyList() {
        final var modifiers = service.generate(ItemRarity.COMMON, 0);

        Assertions.assertEquals(0, modifiers.size());
    }

    @Test
    public void When_Generate1Modifier_Then_ReturnOneModifier() {
        Mockito.when(dao.getRandomModifier(ItemRarity.COMMON)).thenReturn(prefixModifier);
        final var modifiers = service.generate(ItemRarity.COMMON, 1);

        Assertions.assertEquals(1, modifiers.size());
        Assertions.assertEquals(prefixModifier, modifiers.getFirst());}

    @Test
    public void When_Generate2Modifiers_And_FirstIsPrefix_Then_ReturnTwoPrefixes() {
        Mockito.when(dao.getRandomModifier(ItemRarity.COMMON)).thenReturn(prefixModifier);
        Mockito.when(dao.getRandomModifierExcludeId(prefixModifier.id(), ItemRarity.COMMON)).thenReturn(prefixModifier);
        final var modifiers = service.generate(ItemRarity.COMMON, 2);

        Assertions.assertEquals(2, modifiers.size());
        Assertions.assertEquals(prefixModifier, modifiers.get(0));
        Assertions.assertEquals(prefixModifier, modifiers.get(1));
    }

    @Test
    public void When_Generate2Modifiers_And_FirstIsSuffix_Then_ReturnOnePrefixAndOneSuffix() {
        Mockito.when(dao.getRandomModifier(ItemRarity.COMMON)).thenReturn(suffixModifier);
        Mockito.when(dao.getRandomModifierWithType(ModifierType.PREFIX, ItemRarity.COMMON)).thenReturn(prefixModifier);
        final var modifiers = service.generate(ItemRarity.COMMON, 2);

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
