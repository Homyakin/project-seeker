package ru.homyakin.seeker.telegram.command.group.management.settings;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ToggleEventInterval(
    GroupTgId groupTgId,
    UserId userId,
    String callbackId,
    int intervalIndex,
    int messageId
) implements UserGroupCommand {
    public static ToggleEventInterval from(CallbackQuery callback) {
        return new ToggleEventInterval(
            GroupTgId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getId(),
            Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]),
            callback.getMessage().getMessageId()
        );
    }
}
