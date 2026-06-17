package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupInfo(
    GroupTgId groupTgId
) implements GroupCommand {
    public static GroupInfo from(Message message) {
        return new GroupInfo(
            GroupTgId.from(message.getChatId())
        );
    }
}
