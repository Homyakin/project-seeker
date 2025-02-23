package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupCommands(
    GroupTgId groupTgId
) implements Command {
    public static GroupCommands from(Message message) {
        return new GroupCommands(
            GroupTgId.from(message.getChatId())
        );
    }
}
