package ru.homyakin.seeker.telegram.command.user.language;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record UserChangeLanguage(
    Long userId
) implements Command {
    public static UserChangeLanguage from(Message message) {
        return new UserChangeLanguage(message.getFrom().getId());
    }
}
