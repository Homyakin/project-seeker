package ru.homyakin.seeker.game.item;

import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record ItemObject(
    String code,
    ItemType type,
    Optional<Attack> attack,
    Map<Language, ItemObjectLocale> locales
) implements Localized<ItemObjectLocale> {
}
