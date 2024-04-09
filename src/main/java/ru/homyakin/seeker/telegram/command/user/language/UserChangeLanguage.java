package ru.homyakin.seeker.telegram.command.user.language;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserChangeLanguage(
    UserId userId
) implements Command {
    public static UserChangeLanguage from(Message message) {
        return new UserChangeLanguage(UserId.from(message.getFrom().getId()));
    }
}
