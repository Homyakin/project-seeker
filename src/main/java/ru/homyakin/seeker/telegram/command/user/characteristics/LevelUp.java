package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record LevelUp(long userId) implements Command {
    public static LevelUp from(Message message) {
        return new LevelUp(message.getFrom().getId());
    }
}
