package ru.homyakin.seeker.telegram.command.group.duel;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record DeclineDuel(
    String callbackId,
    long groupId,
    UserId userId,
    int messageId,
    long duelId
) implements Command {
    public static DeclineDuel from(CallbackQuery callback) {
        return new DeclineDuel(
            callback.getId(),
            callback.getMessage().getChatId(),
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
