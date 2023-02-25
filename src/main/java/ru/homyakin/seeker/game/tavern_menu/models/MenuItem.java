package ru.homyakin.seeker.game.tavern_menu.models;

import java.util.Collections;
import java.util.List;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.CommandType;
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
        final var locale = getLocaleByLanguageOrDefault(language);
        return  "<b>" + locale.name() + "</b> " + TextConstants.MONEY_ICON + price + " "
            + CommandType.ORDER.getText() + id;
    }

    public String orderText(Language language, Personage personage) {
        final var locale = getLocaleByLanguageOrDefault(language);
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(locale.orderTemplate()),
            Collections.singletonMap("personage_icon_with_name", personage.iconWithName())
        );
    }

    private MenuItemLocale getLocaleByLanguageOrDefault(Language language) {
        MenuItemLocale defaultLocale = null;
        for (final var locale: locales) {
            if (locale.language() == language) {
                return locale;
            }
            if (locale.language() == Language.DEFAULT) {
                defaultLocale = locale;
            }
        }
        if (defaultLocale != null) {
            return defaultLocale;
        }
        throw new IllegalStateException("No default locale for menu item " + id);
    }
}
