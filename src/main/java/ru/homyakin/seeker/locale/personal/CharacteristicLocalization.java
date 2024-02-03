package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.game.personage.models.CharacteristicType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class CharacteristicLocalization {
    private static final Resources<CharacteristicResource> resources = new Resources<>();

    public static void add(Language language, CharacteristicResource resource) {
        resources.add(language, resource);
    }

    public static String resetConfirmation(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::resetConfirmation),
            params
        );
    }

    public static String successReset(Language language) {
        return resources.getOrDefault(language, CharacteristicResource::successReset);
    }

    public static String canceledReset(Language language) {
        return resources.getOrDefault(language, CharacteristicResource::canceledReset);
    }

    public static String increasedCharacteristic(Language language, CharacteristicType type, int value) {
        final var params = new HashMap<String, Object>();
        params.put("increased_value", value);
        params.put("increased_icon", type.icon());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::increasedCharacteristic),
            params
        );
    }

    public static String currentCharacteristics(Language language, Characteristics characteristics) {
        final var params = new HashMap<String, Object>();
        params.put("strength_value", characteristics.strength());
        params.put("strength_icon", CharacteristicType.STRENGTH.icon());
        params.put("agility_value", characteristics.agility());
        params.put("agility_icon", CharacteristicType.AGILITY.icon());
        params.put("wisdom_value", characteristics.wisdom());
        params.put("wisdom_icon", CharacteristicType.WISDOM.icon());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::currentCharacteristics),
            params
        );
    }

    public static String chooseCharacteristic(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("strength_icon", CharacteristicType.STRENGTH.icon());
        params.put("agility_icon", CharacteristicType.AGILITY.icon());
        params.put("wisdom_icon", CharacteristicType.WISDOM.icon());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::chooseCharacteristic),
            params
        );
    }

    public static String notEnoughCharacteristicPoints(Language language) {
        return resources.getOrDefault(language, CharacteristicResource::notEnoughCharacteristicPoints);
    }

    public static String notEnoughMoney(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::notEnoughMoney),
            params
        );
    }

    public static String profileLevelUp(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::profileLevelUp),
            Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
        );
    }

    public static String confirmButton(Language language) {
        return resources.getOrDefault(language, CharacteristicResource::confirmButton);
    }

    public static String cancelButton(Language language) {
        return resources.getOrDefault(language, CharacteristicResource::cancelButton);
    }

    public static String strengthButton(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::strengthButton),
            Collections.singletonMap("strength_icon", CharacteristicType.STRENGTH.icon())
        );
    }

    public static String agilityButton(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::agilityButton),
            Collections.singletonMap("agility_icon", CharacteristicType.AGILITY.icon())
        );
    }

    public static String wisdomButton(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, CharacteristicResource::wisdomButton),
            Collections.singletonMap("wisdom_icon", CharacteristicType.WISDOM.icon())
        );
    }
}
