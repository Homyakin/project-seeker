package ru.homyakin.seeker.telegram.command.user.contraband;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ViewContraband(UserId userId) implements UserCommand {
    public static ViewContraband from(Message message) {
        return new ViewContraband(UserId.from(message.getFrom().getId()));
    }
}
