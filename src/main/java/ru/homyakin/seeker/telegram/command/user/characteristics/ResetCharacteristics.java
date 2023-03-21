package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record ResetCharacteristics(long userId) implements Command {
    public static ResetCharacteristics from(Message message) {
        return new ResetCharacteristics(message.getFrom().getId());
    }
}
