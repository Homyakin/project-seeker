package ru.homyakin.seeker.telegram.command.group.spin;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record SpinTop(long groupId, long userId) implements Command {
    public static SpinTop from(Message message) {
        return new SpinTop(message.getChatId(), message.getFrom().getId());
    }
}
