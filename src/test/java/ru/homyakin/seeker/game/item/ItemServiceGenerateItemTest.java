package ru.homyakin.seeker.game.item;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.characteristics.ItemCharacteristicService;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectGenerateCharacteristics;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.item.rarity.ItemRarityService;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.RandomUtils;

public class ItemServiceGenerateItemTest {
    private final ItemObjectDao itemObjectDao = Mockito.mock(ItemObjectDao.class);
    private final ItemModifierDao itemModifierDao = Mockito.mock(ItemModifierDao.class);
    private final ItemDao itemDao = Mockito.mock(ItemDao.class);
    private final ItemRarityService rarityService = Mockito.mock(ItemRarityService.class);
    private final ItemCharacteristicService characteristicService = Mockito.mock(ItemCharacteristicService.class);
    private final ItemService service = new ItemService(
        itemObjectDao,
        itemModifierDao,
        itemDao,
        characteristicService,
        rarityService
    );

    @BeforeEach
    public void init() {
        Mockito.when(itemObjectDao.getRandomObject(Mockito.any())).thenReturn(object);
    }

    @Test
    public void When_RandomUtilsReturnFalse_Then_GenerateItemWithoutModifiers() {

        Mockito.when(itemDao.getByPersonageId(Mockito.any())).thenReturn(List.of());
        final var item = new Item(
            0L,
            object.toItemObject(),
            ItemRarity.COMMON,
            List.of(),
            Optional.of(personageId),
            false,
            characteristics
        );

        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        Mockito.when(rarityService.generateItemRarity()).thenReturn(ItemRarity.COMMON);
        Mockito.when(characteristicService.createCharacteristics(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            characteristics
        );
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(RandomUtils::bool).thenReturn(false);
            service.generateItemForPersonage(PersonageUtils.withId(personageId));
        }

        final var captor = ArgumentCaptor.forClass(Item.class);
        Mockito.verify(itemDao).saveItem(captor.capture());


        Assertions.assertEquals(item, captor.getValue());
    }

    @Test
    public void When_RandomUtilsReturnTrueAndFalse_Then_GenerateItemWithOneModifier() {
        Mockito.when(itemDao.getByPersonageId(Mockito.any())).thenReturn(List.of());
        Mockito.when(itemModifierDao.getRandomModifier(ItemRarity.COMMON)).thenReturn(modifier1);
        final var item = new Item(
            0L,
            object.toItemObject(),
            ItemRarity.COMMON,
            List.of(modifier1.toModifier()),
            Optional.of(personageId),
            false,
            characteristics
        );

        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        Mockito.when(rarityService.generateItemRarity()).thenReturn(ItemRarity.COMMON);
        Mockito.when(characteristicService.createCharacteristics(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            characteristics
        );
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(RandomUtils::bool).thenReturn(true, false);
            service.generateItemForPersonage(PersonageUtils.withId(personageId));
        }

        final var captor = ArgumentCaptor.forClass(Item.class);
        Mockito.verify(itemDao).saveItem(captor.capture());


        Assertions.assertEquals(item, captor.getValue());
    }

    @Test
    public void When_RandomUtilsReturnTrue_Then_GenerateItemWithTwoModifiers() {
        Mockito.when(itemDao.getByPersonageId(Mockito.any())).thenReturn(List.of());
        Mockito.when(itemModifierDao.getRandomModifier(ItemRarity.COMMON)).thenReturn(modifier1);
        Mockito.when(itemModifierDao.getRandomModifierExcludeId(modifier1.id(), ItemRarity.COMMON)).thenReturn(modifier2);
        final var item = new Item(
            0L,
            object.toItemObject(),
            ItemRarity.COMMON,
            List.of(modifier1.toModifier(), modifier2.toModifier()),
            Optional.of(personageId),
            false,
            characteristics
        );

        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        Mockito.when(rarityService.generateItemRarity()).thenReturn(ItemRarity.COMMON);
        Mockito.when(characteristicService.createCharacteristics(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            characteristics
        );
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(RandomUtils::bool).thenReturn(true);
            service.generateItemForPersonage(PersonageUtils.withId(personageId));
        }

        final var captor = ArgumentCaptor.forClass(Item.class);
        Mockito.verify(itemDao).saveItem(captor.capture());


        Assertions.assertEquals(item, captor.getValue());
    }

    private final PersonageId personageId = PersonageId.from(0L);
    private final GenerateItemObject object = new GenerateItemObject(
        0,
        "",
        Set.of(PersonageSlot.MAIN_HAND),
        new ObjectGenerateCharacteristics(List.of()),
        Collections.emptyMap()
    );
    private final GenerateModifier modifier1 = new GenerateModifier(
        0,
        "",
        ModifierType.PREFIX,
        new ModifierGenerateCharacteristics(List.of()),
        Collections.emptyMap()
    );
    private final GenerateModifier modifier2 = new GenerateModifier(
        1,
        "",
        ModifierType.PREFIX,
        new ModifierGenerateCharacteristics(List.of()),
        Collections.emptyMap()
    );
    private final Characteristics characteristics = new Characteristics(
        0,
        1,
        0,
        0,
        0,
        0
    );
}
