package ru.homyakin.seeker.telegram.command.group.trigger;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record TryCallTrigger(
        GroupId groupId,
        String possibleTextToTrigger
) implements Command {

    public static TryCallTrigger from(Message message) {
        return new TryCallTrigger(
                GroupId.from(message.getChatId()),
                message.getText()
        );
    }

}
