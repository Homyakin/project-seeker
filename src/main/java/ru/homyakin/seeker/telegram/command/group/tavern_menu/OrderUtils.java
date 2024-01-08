package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public class OrderUtils {
    public static String getMenuItemCode(String commandText) {
        return commandText.split("@")[0].replace(CommandType.ORDER.getText() + TextConstants.TG_COMMAND_DELIMITER, "");
    }
}
