package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemLocale;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingMenuItem(
    String code,
    Money price,
    boolean isAvailable,
    Category category,
    Map<Language, MenuItemLocale> locales
) implements Localized<MenuItemLocale> {
}
