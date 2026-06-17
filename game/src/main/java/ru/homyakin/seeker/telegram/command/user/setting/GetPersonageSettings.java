package ru.homyakin.seeker.telegram.command.user.setting;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetPersonageSettings(
    UserId userId
) implements UserCommand {
    public static GetPersonageSettings from(Message message) {
        return new GetPersonageSettings(
            UserId.from(message.getFrom().getId())
        );
    }
}
