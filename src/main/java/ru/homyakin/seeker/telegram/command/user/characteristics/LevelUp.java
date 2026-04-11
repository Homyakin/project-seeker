package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LevelUp(UserId userId) implements UserCommand {
    public static LevelUp from(Message message) {
        return new LevelUp(UserId.from(message.getFrom().getId()));
    }
}
