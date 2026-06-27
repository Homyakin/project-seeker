package ru.homyakin.seeker.telegram.command.user.profile;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetProfileInPrivate(
    UserId userId
) implements UserCommand {
    public static GetProfileInPrivate from(Message message) {
        return new GetProfileInPrivate(UserId.from(message.getFrom().getId()));
    }

    public static GetProfileInPrivate from(CallbackQuery callback) {
        return new GetProfileInPrivate(UserId.from(callback.getFrom().getId()));
    }
}
