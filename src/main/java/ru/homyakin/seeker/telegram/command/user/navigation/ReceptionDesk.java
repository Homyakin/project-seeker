package ru.homyakin.seeker.telegram.command.user.navigation;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ReceptionDesk(UserId userId) implements Command {
    public static ReceptionDesk from(Message message) {
        return new ReceptionDesk(UserId.from(message.getFrom().getId()));
    }
}
