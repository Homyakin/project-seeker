package ru.homyakin.seeker.telegram.command.group.raid;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record JoinRaid(
    String callbackId,
    GroupTgId groupId,
    Integer messageId,
    UserId userId,
    long launchedEventId
) implements Command {
    public static JoinRaid from(CallbackQuery callback) {
        return new JoinRaid(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
