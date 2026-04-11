package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OutpostOpenBuildPicker(UserId userId, int messageId, String callbackId) implements UserCommand {
    public static OutpostOpenBuildPicker from(CallbackQuery callback) {
        return new OutpostOpenBuildPicker(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
