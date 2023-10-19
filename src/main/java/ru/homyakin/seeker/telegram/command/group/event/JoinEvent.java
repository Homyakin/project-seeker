package ru.homyakin.seeker.telegram.command.group.event;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record JoinEvent(
    String callbackId,
    GroupId groupId,
    Integer messageId,
    UserId userId,
    long launchedEventId
) implements Command {
    public static JoinEvent from(CallbackQuery callback) {
        return new JoinEvent(
            callback.getId(),
            GroupId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
