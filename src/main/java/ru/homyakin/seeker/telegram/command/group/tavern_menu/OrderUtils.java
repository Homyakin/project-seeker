package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.telegram.command.type.CommandType;

import java.util.Optional;

public class OrderUtils {
    public static Optional<Integer> getMenuItemId(String commandText) {
        try {
            return Optional.of(
                Integer.parseInt(commandText.split("@")[0].replace(CommandType.ORDER.getText(), ""))
            );
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
