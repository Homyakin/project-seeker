package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GetTavernMenu(GroupId groupId) implements Command {
    public static GetTavernMenu from(Message message) {
        return new GetTavernMenu(GroupId.from(message.getChatId()));
    }
}
