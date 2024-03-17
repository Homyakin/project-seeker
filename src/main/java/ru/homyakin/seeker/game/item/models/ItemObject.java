package ru.homyakin.seeker.game.item.models;

import java.util.Map;
import java.util.Set;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record ItemObject(
    String code,
    Set<PersonageSlot> slots,
    ItemRangeCharacteristics characteristics,
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
}
