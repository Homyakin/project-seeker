package ru.homyakin.seeker.game.item.modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifierType;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;

import java.util.Map;

public class ItemModifierServiceTest {
    private final LegacyItemModifierDao dao = Mockito.mock(LegacyItemModifierDao.class);
    private final LegacyItemModifierService service = new LegacyItemModifierService(dao);

    @Test
    public void When_GenerateZeroModifiers_Then_ReturnEmptyList() {
        final var modifiers = service.generate(LegacyItemRarity.COMMON, 0);

        Assertions.assertEquals(0, modifiers.size());
    }

    @Test
    public void When_Generate1Modifier_Then_ReturnOneModifier() {
        Mockito.when(dao.getRandomModifier(LegacyItemRarity.COMMON)).thenReturn(prefixModifier);
        final var modifiers = service.generate(LegacyItemRarity.COMMON, 1);

        Assertions.assertEquals(1, modifiers.size());
        Assertions.assertEquals(prefixModifier, modifiers.getFirst());}

    @Test
    public void When_Generate2Modifiers_And_FirstIsPrefix_Then_ReturnTwoPrefixes() {
        Mockito.when(dao.getRandomModifier(LegacyItemRarity.COMMON)).thenReturn(prefixModifier);
        Mockito.when(dao.getRandomModifierExcludeId(prefixModifier.id(), LegacyItemRarity.COMMON)).thenReturn(prefixModifier);
        final var modifiers = service.generate(LegacyItemRarity.COMMON, 2);

        Assertions.assertEquals(2, modifiers.size());
        Assertions.assertEquals(prefixModifier, modifiers.get(0));
        Assertions.assertEquals(prefixModifier, modifiers.get(1));
    }

    @Test
    public void When_Generate2Modifiers_And_FirstIsSuffix_Then_ReturnOnePrefixAndOneSuffix() {
        Mockito.when(dao.getRandomModifier(LegacyItemRarity.COMMON)).thenReturn(suffixModifier);
        Mockito.when(dao.getRandomModifierWithType(LegacyModifierType.PREFIX, LegacyItemRarity.COMMON)).thenReturn(prefixModifier);
        final var modifiers = service.generate(LegacyItemRarity.COMMON, 2);

        Assertions.assertEquals(2, modifiers.size());
        Assertions.assertEquals(suffixModifier, modifiers.get(0));
        Assertions.assertEquals(prefixModifier, modifiers.get(1));
    }

    private final LegacyGenerateModifier prefixModifier = new LegacyGenerateModifier(
        1,
        "",
        LegacyModifierType.PREFIX,
        Mockito.mock(ModifierGenerateCharacteristics.class),
        Map.of()
    );

    private final LegacyGenerateModifier suffixModifier = new LegacyGenerateModifier(
        1,
        "",
        LegacyModifierType.SUFFIX,
        Mockito.mock(ModifierGenerateCharacteristics.class),
        Map.of()
    );
}
