package ru.homyakin.seeker.infrastructure.init.saving_models.item;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectGenerateCharacteristics;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingItemObject(
    @JsonProperty(required = true)
    String code,
    @JsonProperty(required = true)
    Set<ItemRarity> rarities,
    @JsonProperty(required = true)
    Set<PersonageSlot> slots,
    @JsonProperty(required = true)
    ObjectGenerateCharacteristics characteristics,
    @JsonProperty(required = true)
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
}
