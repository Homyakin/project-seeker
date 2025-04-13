package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TakePersonalQuest(
    UserId userId
) implements Command {
    public static TakePersonalQuest from(Message message) {
        return new TakePersonalQuest(UserId.from(message.getFrom().getId()));
    }
}
