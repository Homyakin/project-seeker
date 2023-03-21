package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record GetTavernMenu(long groupId) implements Command {
    public static GetTavernMenu from(Message message) {
        return new GetTavernMenu(message.getChatId());
    }
}
