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
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemGenerateCharacteristics;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.IntRange;

public class ItemServiceGenerateItemTest {
    private final ItemObjectDao itemObjectDao = Mockito.mock(ItemObjectDao.class);
    private final ItemModifierDao itemModifierDao = Mockito.mock(ItemModifierDao.class);
    private final ItemDao itemDao = Mockito.mock(ItemDao.class);
    private final ItemService service = new ItemService(itemObjectDao, itemModifierDao, itemDao);

    @BeforeEach
    public void init() {
        Mockito.when(itemObjectDao.getRandomObject()).thenReturn(object);
    }

    @Test
    public void When_RandomUtilsReturnFalse_Then_GenerateItemWithoutModifiers() {
        // when
        Mockito.when(itemDao.getByPersonageId(Mockito.any())).thenReturn(List.of());
        final var item = new Item(
            0L,
            object.toItemObject(),
            List.of(),
            Optional.of(personageId),
            false,
            new Characteristics(
                0,
                1,
                0,
                0,
                0,
                0
            )
        );
        Mockito.when(itemDao.getById(Mockito.any())).thenReturn(Optional.of(item));
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(RandomUtils::bool).thenReturn(false);
            mock.when(() -> RandomUtils.getCharacteristic(Mockito.any(Integer.class), Mockito.any(Integer.class)))
                .thenCallRealMethod();
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
        new ItemGenerateCharacteristics(
            Optional.of(new IntRange(1, 1)),
            Optional.empty()
        ),
        Collections.emptyMap()
    );
}
