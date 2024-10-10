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
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.modifier.ItemModifierService;
import ru.homyakin.seeker.game.item.modifier.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.ModifierType;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.RandomUtils;

public class ItemServiceGenerateItemTest {
    private final ItemObjectDao itemObjectDao = Mockito.mock(ItemObjectDao.class);
    private final ItemModifierService itemModifierService = Mockito.mock(ItemModifierService.class);
    private final ItemDao itemDao = Mockito.mock(ItemDao.class);
    private final ItemCharacteristicService characteristicService = Mockito.mock(ItemCharacteristicService.class);
    private final ItemService service = new ItemService(
        itemObjectDao,
        itemModifierService,
        itemDao,
        characteristicService
    );

    @BeforeEach
    public void init() {
        Mockito.when(itemObjectDao.getRandomObject(Mockito.any(), Mockito.any())).thenReturn(object);
    }

    @Test
    public void When_ModifierServiceReturnEmptyList_And_ObjectDaoReturnCommonObject_Then_GenerateCommonItemWithoutModifiers() {

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
        final var params = new GenerateItemParams(
            ItemRarity.COMMON,
            PersonageSlot.MAIN_HAND,
            0
        );

        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        Mockito.when(characteristicService.createCharacteristics(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            characteristics
        );
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(RandomUtils::bool).thenReturn(false);
            service.generateItemForPersonage(PersonageUtils.withId(personageId), params);
        }

        final var captor = ArgumentCaptor.forClass(Item.class);
        Mockito.verify(itemDao).saveItem(captor.capture());


        Assertions.assertEquals(item, captor.getValue());
    }

    @Test
    public void When_ModifierServiceReturnTwoModifiers_And_ObjectDaoReturnCommonObject_Then_GenerateCommonItemWithTwoModifiers() {
        Mockito.when(itemDao.getByPersonageId(Mockito.any())).thenReturn(List.of());
        Mockito.when(itemModifierService.generate(ItemRarity.COMMON, 2)).thenReturn(
            List.of(modifier1, modifier2)
        );
        final var item = new Item(
            0L,
            object.toItemObject(),
            ItemRarity.COMMON,
            List.of(modifier1.toModifier(), modifier2.toModifier()),
            Optional.of(personageId),
            false,
            characteristics
        );
        final var params = new GenerateItemParams(
            ItemRarity.COMMON,
            PersonageSlot.MAIN_HAND,
            2
        );

        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        Mockito.when(characteristicService.createCharacteristics(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            characteristics
        );

        service.generateItemForPersonage(PersonageUtils.withId(personageId), params);

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
