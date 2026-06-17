package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenOutpostMenu(UserId userId) implements UserCommand {
    public static OpenOutpostMenu from(Message message) {
        return new OpenOutpostMenu(UserId.from(message.getFrom().getId()));
    }
}
