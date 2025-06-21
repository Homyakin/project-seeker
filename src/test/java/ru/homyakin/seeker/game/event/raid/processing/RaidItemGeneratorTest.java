package ru.homyakin.seeker.game.event.raid.processing;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.random.item.action.PersonageNextRaidItemParams;
import ru.homyakin.seeker.utils.RandomUtils;

public class RaidItemGeneratorTest {
    private final PersonageService personageService = Mockito.mock();
    private final ItemService itemService = Mockito.mock();
    private final PersonageNextRaidItemParams personageNextRaidItemParams = Mockito.mock();
    private final RaidItemGenerator generator = new RaidItemGenerator(personageService, itemService, personageNextRaidItemParams);

    @Test
    public void Given_Lose_Then_ReturnEmpty() {
        final var result = generator.generateItem(false, Mockito.mock(), true);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void Given_IsExhausted_Then_ReturnEmpty() {
        final var result = generator.generateItem(true, Mockito.mock(), false);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void Given_WinAndNotExhausted_When_ProcessChanceIsFalse_Then_ReturnEmpty() {
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(Mockito.anyInt())).thenReturn(false);
            final var result = generator.generateItem(true, Mockito.mock(), false);
            Assertions.assertTrue(result.isEmpty());
        }

    }

    @Test
    public void Given_WinAndNotExhausted_When_ProcessChanceIsTrue_Then_ReturnItem() {
        final var expectedItem = Mockito.mock(Item.class);

        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(Mockito.anyInt())).thenReturn(true);
            Mockito.when(personageNextRaidItemParams.get(Mockito.any())).thenReturn(Mockito.mock());
            Mockito.when(itemService.generateItemForPersonage(Mockito.any(), Mockito.any())).thenReturn(Either.right(expectedItem));

            final var result = generator.generateItem(true, Mockito.mock(), false);

            Assertions.assertFalse(result.isEmpty());
            Assertions.assertInstanceOf(GeneratedItemResult.Success.class, result.get());
            Assertions.assertEquals(expectedItem, ((GeneratedItemResult.Success) result.get()).item());
        }
    }
}
