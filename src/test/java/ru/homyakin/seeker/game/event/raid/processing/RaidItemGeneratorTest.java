package ru.homyakin.seeker.game.event.raid.processing;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.event.raid.models.GeneratedItemResult;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.action.PersonageNextRaidItemParams;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

public class RaidItemGeneratorTest {
    private final PersonageService personageService = Mockito.mock();
    private final ItemService itemService = Mockito.mock();
    private final PersonageNextRaidItemParams personageNextRaidItemParams = Mockito.mock();
    private final ContrabandService contrabandService = Mockito.mock();
    private final RaidItemGenerator generator = new RaidItemGenerator(
        personageService,
        itemService,
        personageNextRaidItemParams,
        contrabandService
    );

    @Test
    public void Given_Lose_Then_ReturnEmpty() {
        final var result = generator.generateItem(false, Mockito.mock(), true, 10, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void Given_IsExhausted_Then_ReturnEmpty() {
        final var result = generator.generateItem(true, Mockito.mock(), true, 10, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void Given_WinAndNotExhausted_When_ProcessChanceIsFalse_Then_ReturnEmpty() {
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(Mockito.anyInt())).thenReturn(false);
            Mockito.when(contrabandService.tryCreate(Mockito.any(), Mockito.anyInt()))
                .thenReturn(Optional.empty());
            final var result = generator.generateItem(true, personage(), false, 10, 0);
            Assertions.assertTrue(result.isEmpty());
        }

    }

    @Test
    public void Given_WinAndNotExhausted_When_ProcessChanceIsTrue_Then_ReturnItem() {
        final var expectedItem = Mockito.mock(PersonageItem.class);

        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(Mockito.anyInt())).thenReturn(true);
            Mockito.when(contrabandService.tryCreate(Mockito.any(), Mockito.anyInt()))
                .thenReturn(Optional.empty());
            Mockito.when(personageNextRaidItemParams.get(Mockito.any(), Mockito.anyInt()))
                .thenReturn(new ItemParamsFull(ItemRarity.COMMON, PersonageSlot.MAIN_HAND));
            Mockito.when(itemService.generateItemForPersonage(Mockito.any(), Mockito.<GenerateItemParams>any()))
                .thenReturn(Either.right(expectedItem));

            final var result = generator.generateItem(true, personage(), false, 10, 0);

            Assertions.assertFalse(result.isEmpty());
            Assertions.assertInstanceOf(GeneratedItemResult.Success.class, result.get());
            Assertions.assertEquals(expectedItem, ((GeneratedItemResult.Success) result.get()).item());
        }
    }

    private Personage personage() {
        final var personage = Mockito.mock(Personage.class);
        Mockito.when(personage.id()).thenReturn(PersonageId.from(1));
        return personage;
    }
}
