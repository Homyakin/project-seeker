package ru.homyakin.seeker.telegram.command.user.navigation;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record ReceptionDesk(long userId) implements Command {
    public static ReceptionDesk from(Message message) {
        return new ReceptionDesk(message.getFrom().getId());
    }
}
