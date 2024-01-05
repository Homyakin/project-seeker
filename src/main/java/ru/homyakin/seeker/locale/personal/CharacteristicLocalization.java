package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.game.personage.models.CharacteristicType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class CharacteristicLocalization {
    private static final Map<Language, CharacteristicResource> map = new HashMap<>();

    public static void add(Language language, CharacteristicResource resource) {
        map.put(language, resource);
    }

    public static String resetConfirmation(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(
                map.get(language).resetConfirmation(),
                map.get(Language.DEFAULT).resetConfirmation()
            ),
            params
        );
    }

    public static String successReset(Language language) {
        return CommonUtils.ifNullThen(map.get(language).successReset(), map.get(Language.DEFAULT).successReset());
    }

    public static String canceledReset(Language language) {
        return CommonUtils.ifNullThen(map.get(language).canceledReset(), map.get(Language.DEFAULT).canceledReset());
    }

    public static String increasedCharacteristic(Language language, CharacteristicType type, int value) {
        final var params = new HashMap<String, Object>();
        params.put("increased_value", value);
        params.put("increased_icon", type.icon());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).increasedCharacteristic(), map.get(Language.DEFAULT).increasedCharacteristic()),
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
            CommonUtils.ifNullThen(map.get(language).currentCharacteristics(), map.get(Language.DEFAULT).currentCharacteristics()),
            params
        );
    }

    public static String chooseCharacteristic(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("strength_icon", CharacteristicType.STRENGTH.icon());
        params.put("agility_icon", CharacteristicType.AGILITY.icon());
        params.put("wisdom_icon", CharacteristicType.WISDOM.icon());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).chooseCharacteristic(), map.get(Language.DEFAULT).chooseCharacteristic()),
            params
        );
    }

    public static String notEnoughCharacteristicPoints(Language language) {
        return CommonUtils.ifNullThen(
            map.get(language).notEnoughCharacteristicPoints(),
            map.get(Language.DEFAULT).notEnoughCharacteristicPoints()
        );
    }

    public static String notEnoughMoney(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).notEnoughMoney(), map.get(Language.DEFAULT).notEnoughMoney()),
            params
        );
    }

    public static String profileLevelUp(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).profileLevelUp(), map.get(Language.DEFAULT).profileLevelUp()),
            Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
        );
    }

    public static String confirmButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).confirmButton(), map.get(Language.DEFAULT).confirmButton());
    }

    public static String cancelButton(Language language) {
        return CommonUtils.ifNullThen(map.get(language).cancelButton(), map.get(Language.DEFAULT).cancelButton());
    }

    public static String strengthButton(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).strengthButton(), map.get(Language.DEFAULT).strengthButton()),
            Collections.singletonMap("strength_icon", CharacteristicType.STRENGTH.icon())
        );
    }

    public static String agilityButton(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).agilityButton(), map.get(Language.DEFAULT).agilityButton()),
            Collections.singletonMap("agility_icon", CharacteristicType.AGILITY.icon())
        );
    }

    public static String wisdomButton(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThen(map.get(language).wisdomButton(), map.get(Language.DEFAULT).wisdomButton()),
            Collections.singletonMap("wisdom_icon", CharacteristicType.WISDOM.icon())
        );
    }
}
