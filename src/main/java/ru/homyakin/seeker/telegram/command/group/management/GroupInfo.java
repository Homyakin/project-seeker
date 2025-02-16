package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupInfo(
    GroupTgId groupTgId
) implements Command {
    public static GroupInfo from(Message message) {
        return new GroupInfo(
            GroupTgId.from(message.getChatId())
        );
    }
}
