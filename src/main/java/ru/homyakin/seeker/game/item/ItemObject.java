package ru.homyakin.seeker.game.item;

import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.utils.models.IntRange;

public record ItemObject(
    String code,
    ItemType type,
    Optional<IntRange> attack,
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
}
