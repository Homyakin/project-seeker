package ru.homyakin.seeker.telegram.command.group.spin;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record Spin(long groupId, UserId userId) implements Command {
    public static Spin from(Message message) {
        return new Spin(message.getChatId(), UserId.from(message.getFrom().getId()));
    }
}
