package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LevelUp(UserId userId) implements Command {
    public static LevelUp from(Message message) {
        return new LevelUp(UserId.from(message.getFrom().getId()));
    }
}
