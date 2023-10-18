package ru.homyakin.seeker.telegram.command.user.navigation;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record Back(UserId userId) implements Command {
    public static Back from(Message message) {
        return new Back(UserId.from(message.getFrom().getId()));
    }
}
