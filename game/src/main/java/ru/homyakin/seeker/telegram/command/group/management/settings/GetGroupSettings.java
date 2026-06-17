package ru.homyakin.seeker.telegram.command.group.management.settings;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GetGroupSettings(
    GroupTgId groupTgId
) implements GroupCommand {
    public static GetGroupSettings from(Message message) {
        return new GetGroupSettings(GroupTgId.from(message.getChatId()));
    }
}
