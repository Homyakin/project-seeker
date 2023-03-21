package ru.homyakin.seeker.telegram.command.user.profile;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record GetProfileInPrivate(
    Long userId
) implements Command {
    public static GetProfileInPrivate from(Message message) {
        return new GetProfileInPrivate(message.getFrom().getId());
    }
}
