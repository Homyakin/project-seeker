package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ResetCharacteristics(UserId userId) implements UserCommand {
    public static ResetCharacteristics from(Message message) {
        return new ResetCharacteristics(UserId.from(message.getFrom().getId()));
    }
}
