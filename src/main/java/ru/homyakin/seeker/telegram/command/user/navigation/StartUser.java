package ru.homyakin.seeker.telegram.command.user.navigation;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record StartUser(
    Long userId
) implements Command {
    public static StartUser from(Message message) {
        return new StartUser(message.getFrom().getId());
    }
}
