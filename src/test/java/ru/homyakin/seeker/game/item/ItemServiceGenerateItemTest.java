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
import ru.homyakin.seeker.game.item.characteristics.LegacyItemCharacteristicService;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.database.LegacyItemDao;
import ru.homyakin.seeker.game.item.database.LegacyItemObjectDao;
import ru.homyakin.seeker.game.item.models.LegacyGenerateItemObject;
import ru.homyakin.seeker.game.item.models.LegacyGenerateItemParams;
import ru.homyakin.seeker.game.item.modifier.LegacyItemModifierService;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.models.LegacyItem;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifierType;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.RandomUtils;

public class ItemServiceGenerateItemTest {
    private final LegacyItemObjectDao itemObjectDao = Mockito.mock(LegacyItemObjectDao.class);
    private final LegacyItemModifierService itemModifierService = Mockito.mock(LegacyItemModifierService.class);
    private final LegacyItemDao itemDao = Mockito.mock(LegacyItemDao.class);
    private final LegacyItemCharacteristicService characteristicService = Mockito.mock(LegacyItemCharacteristicService.class);
    private final LegacyItemService service = new LegacyItemService(
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
        final var item = new LegacyItem(
            0L,
            object.toItemObject(),
            LegacyItemRarity.COMMON,
            List.of(),
            Optional.of(personageId),
            false,
            false,
            characteristics
        );
        final var params = new LegacyGenerateItemParams(
            LegacyItemRarity.COMMON,
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

        final var captor = ArgumentCaptor.forClass(LegacyItem.class);
        Mockito.verify(itemDao).saveItem(captor.capture());


        Assertions.assertEquals(item, captor.getValue());
    }

    @Test
    public void When_ModifierServiceReturnTwoModifiers_And_ObjectDaoReturnCommonObject_Then_GenerateCommonItemWithTwoModifiers() {
        Mockito.when(itemDao.getByPersonageId(Mockito.any())).thenReturn(List.of());
        Mockito.when(itemModifierService.generate(LegacyItemRarity.COMMON, 2)).thenReturn(
            List.of(modifier1, modifier2)
        );
        final var item = new LegacyItem(
            0L,
            object.toItemObject(),
            LegacyItemRarity.COMMON,
            List.of(modifier1.toModifier(), modifier2.toModifier()),
            Optional.of(personageId),
            false,
            false,
            characteristics
        );
        final var params = new LegacyGenerateItemParams(
            LegacyItemRarity.COMMON,
            PersonageSlot.MAIN_HAND,
            2
        );

        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        Mockito.when(characteristicService.createCharacteristics(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
            characteristics
        );

        service.generateItemForPersonage(PersonageUtils.withId(personageId), params);

        final var captor = ArgumentCaptor.forClass(LegacyItem.class);
        Mockito.verify(itemDao).saveItem(captor.capture());


        Assertions.assertEquals(item, captor.getValue());
    }

    private final PersonageId personageId = PersonageId.from(0L);
    private final LegacyGenerateItemObject object = new LegacyGenerateItemObject(
        0,
        "",
        Set.of(PersonageSlot.MAIN_HAND),
        new ObjectGenerateCharacteristics(List.of()),
        Collections.emptyMap()
    );
    private final LegacyGenerateModifier modifier1 = new LegacyGenerateModifier(
        0,
        "",
        LegacyModifierType.PREFIX,
        new ModifierGenerateCharacteristics(List.of()),
        Collections.emptyMap()
    );
    private final LegacyGenerateModifier modifier2 = new LegacyGenerateModifier(
        1,
        "",
        LegacyModifierType.PREFIX,
        new ModifierGenerateCharacteristics(List.of()),
        Collections.emptyMap()
    );
    private final Characteristics characteristics = new Characteristics(
        0,
        1,
        0
    );
}
