package ru.homyakin.seeker.telegram.command.group.trigger;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record DelTrigger(
        GroupId groupId,
        String textToTrigger
) implements Command {

    public static DelTrigger from(Message message) {
        final var textToTrigger = message.getText()
                .replaceAll(CommandType.DEL_TRIGGER.getText(), "")
                .trim();

        return new DelTrigger(
                GroupId.from(message.getChatId()),
                textToTrigger
        );
    }

}
