package ru.homyakin.seeker.game.tavern_menu.menu.models;

import java.util.Collections;
import java.util.Map;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public record MenuItem(
    int id,
    String code,
    Money price,
    boolean isAvailable,
    Category category,
    Map<Language, MenuItemLocale> locales,
    Effect effect
) implements Localized<MenuItemLocale> {
    // TODO в локализацию
    public String menuPositionText(Language language) {
        return  "<b>" + name(language) + "</b> " + Icons.MONEY + price.value() + " "
            + CommandType.ORDER.getText() + TextConstants.TG_COMMAND_DELIMITER + code;
    }

    public String name(Language language) {
        return getLocaleOrDefault(language).name();
    }

    public String consumeText(Language language, Personage personage) {
        final var locale = getLocaleOrDefault(language);
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(locale.consumeTemplate()),
            Collections.singletonMap("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage))
        );
    }
}
