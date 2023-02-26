package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class LevelingLocalization {
    private static final Map<Language, LevelingResource> map = new HashMap<>();

    public static void add(Language language, LevelingResource resource) {
        map.put(language, resource);
    }

    public static String profileLevelUp(Language language) {
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).profileLevelUp(), map.get(Language.DEFAULT).profileLevelUp()),
            Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
        );
    }

    public static String notEnoughLevelingPoints(Language language) {
        return CommonUtils.ifNullThan(map.get(language).notEnoughLevelingPoints(), map.get(Language.DEFAULT).notEnoughLevelingPoints());
    }

    public static String chooseLevelUpCharacteristic(Language language) {
        return CommonUtils.ifNullThan(
            map.get(language).chooseLevelUpCharacteristic(), map.get(Language.DEFAULT).chooseLevelUpCharacteristic()
        );
    }

    public static String successLevelUp(Language language) {
        return CommonUtils.ifNullThan(map.get(language).successLevelUp(), map.get(Language.DEFAULT).successLevelUp());
    }

    public static String successResetStats(Language language) {
        return CommonUtils.ifNullThan(map.get(language).successResetStats(), map.get(Language.DEFAULT).successResetStats());
    }

    public static String notEnoughMoney(Language language, Money money) {
        final var params = Map.<String, Object>of(
            "money_icon", TextConstants.MONEY_ICON,
            "money_count", money.value()
        );
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).notEnoughMoney(), map.get(Language.DEFAULT).notEnoughMoney()),
            params
        );
    }
}
