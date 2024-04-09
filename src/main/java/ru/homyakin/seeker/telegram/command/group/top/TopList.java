package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record TopList(
    GroupId groupId
) implements Command {
    public static TopList from(Message message) {
        return new TopList(
            GroupId.from(message.getChatId())
        );
    }
}
