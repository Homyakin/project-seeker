package ru.homyakin.seeker.telegram.command.group.duel;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public record AcceptDuel(
    String callbackId,
    GroupTgId groupId,
    UserId userId,
    int messageId,
    long duelId,
    String currentText
) implements ProcessDuel {
    public static AcceptDuel from(CallbackQuery callback) {
        return new AcceptDuel(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]),
            TelegramUtils.validateCallbackMessage(callback).getText()
        );
    }
}
