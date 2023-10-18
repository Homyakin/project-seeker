package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ResetCharacteristics(UserId userId) implements Command {
    public static ResetCharacteristics from(Message message) {
        return new ResetCharacteristics(UserId.from(message.getFrom().getId()));
    }
}
