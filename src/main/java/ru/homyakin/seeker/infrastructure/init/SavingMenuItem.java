package ru.homyakin.seeker.infrastructure.init;

import java.util.List;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemLocale;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;

public record SavingMenuItem(
    String code,
    Money price,
    boolean isAvailable,
    Category category,
    List<MenuItemLocale> locales
) {

    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at menu item " + code);
        }
    }
}
