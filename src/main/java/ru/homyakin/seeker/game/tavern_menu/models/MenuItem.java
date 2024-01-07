package ru.homyakin.seeker.game.tavern_menu.models;

import java.util.Collections;
import java.util.List;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public record MenuItem(
    int id,
    Money price,
    boolean isAvailable,
    Category category,
    List<MenuItemLocale> locales
) {
    public String menuPositionText(Language language) {
        return  "<b>" + name(language) + "</b> " + Icons.MONEY + price.value() + " "
            + CommandType.ORDER.getText() + id;
    }

    public String name(Language language) {
        return getLocaleByLanguageOrDefault(language).name();
    }

    public String consumeText(Language language, Personage personage) {
        final var locale = getLocaleByLanguageOrDefault(language);
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(locale.consumeTemplate()),
            Collections.singletonMap("personage_icon_with_name", personage.iconWithName())
        );
    }

    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at menu item " + id);
        }
    }

    private MenuItemLocale getLocaleByLanguageOrDefault(Language language) {
        return LocaleUtils.getLocaleByLanguageOrDefault(locales, language)
            .orElseThrow(() -> new IllegalStateException("No default locale for menu item " + id));
    }
}
