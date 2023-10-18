package ru.homyakin.seeker.telegram.command.user.profile;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetProfileInPrivate(
    UserId userId
) implements Command {
    public static GetProfileInPrivate from(Message message) {
        return new GetProfileInPrivate(UserId.from(message.getFrom().getId()));
    }
}
