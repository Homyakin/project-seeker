package ru.homyakin.seeker.telegram.command.user.setting;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ToggleHidePersonage(
    UserId userId
) implements Command {
    public static ToggleHidePersonage from(Message message) {
        return new ToggleHidePersonage(UserId.from(message.getFrom().getId()));
    }
}
