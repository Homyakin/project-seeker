package ru.homyakin.seeker.telegram.command.user.navigation;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record Back(long userId) implements Command {
    public static Back from(Message message) {
        return new Back(message.getFrom().getId());
    }
}
