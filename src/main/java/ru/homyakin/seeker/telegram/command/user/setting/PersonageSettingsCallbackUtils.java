package ru.homyakin.seeker.telegram.command.user.setting;

import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.models.Pair;

public class PersonageSettingsCallbackUtils {

    /**
     * Возвращает callback для переключения настроек персонажа
     * Формат: commandType + DELIMITER + setting + DELIMITER + newValue
     */
    public static String createCallback(PersonageSetting setting, boolean currentValue) {
        return CommandType.TOGGLE_PERSONAGE_SETTING.getText()
            + TextConstants.CALLBACK_DELIMITER
            + settingToCallback(setting)
            + TextConstants.CALLBACK_DELIMITER
            + valueToCallback(currentValue);
    }

    public static Pair<PersonageSetting, Boolean> parseCallback(String callback) {
        final var split = callback.split(TextConstants.CALLBACK_DELIMITER);
        final var setting = switch (split[1]) {
            case NOTIFICATIONS -> PersonageSetting.SEND_NOTIFICATIONS;
            case AUTO_QUESTING -> PersonageSetting.AUTO_QUESTING;
            default -> throw new IllegalArgumentException("Unknown setting " + split[1]);
        };
        final var value = split[2].equals(TRUE);
        return new Pair<>(setting, value);
    }

    private static String settingToCallback(PersonageSetting setting) {
        return switch (setting) {
            case SEND_NOTIFICATIONS -> NOTIFICATIONS;
            case AUTO_QUESTING -> AUTO_QUESTING;
        };
    }

    private static String valueToCallback(boolean currentValue) {
        if (currentValue) {
            return FALSE;
        } else {
            return TRUE;
        }
    }

    private static final String NOTIFICATIONS = "notifications";
    private static final String AUTO_QUESTING = "autoQuesting";
    private static final String TRUE = "t";
    private static final String FALSE = "f";
}
