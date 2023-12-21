package ru.homyakin.seeker.telegram.command.group.duel;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record DeclineDuel(
    String callbackId,
    GroupId groupId,
    UserId userId,
    int messageId,
    long duelId,
    String currentText
) implements ProcessDuel {
    public static DeclineDuel from(CallbackQuery callback) {
        return new DeclineDuel(
            callback.getId(),
            GroupId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]),
            callback.getMessage().getText()
        );
    }
}
