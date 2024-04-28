package ru.homyakin.seeker.telegram.command.group.settings;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ToggleEventInterval(
    GroupId groupId,
    UserId userId,
    String callbackId,
    int intervalIndex,
    int messageId
) implements Command {
    public static ToggleEventInterval from(CallbackQuery callback) {
        return new ToggleEventInterval(
            GroupId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getId(),
            Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]),
            callback.getMessage().getMessageId()
        );
    }
}
