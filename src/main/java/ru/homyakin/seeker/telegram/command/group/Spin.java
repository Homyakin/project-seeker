package ru.homyakin.seeker.telegram.command.group;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record Spin(long groupId, long userId) implements Command {
    public static Spin from(Message message) {
        return new Spin(message.getChatId(), message.getFrom().getId());
    }
}
