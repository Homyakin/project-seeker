package ru.homyakin.seeker.infrastructure.init.saving_models.item;

import java.util.Map;
import java.util.Set;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.item.models.ItemRangeCharacteristics;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingItemObject(
    String code,
    Set<PersonageSlot> slots,
    ItemRangeCharacteristics characteristics,
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
}
