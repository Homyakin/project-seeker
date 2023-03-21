package ru.homyakin.seeker.telegram.command.group.event;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record JoinEvent(
    String callbackId,
    Long groupId,
    Integer messageId,
    Long userId,
    long launchedEventId
) implements Command {
    public static JoinEvent from(CallbackQuery callback) {
        return new JoinEvent(
            callback.getId(),
            callback.getMessage().getChatId(),
            callback.getMessage().getMessageId(),
            callback.getFrom().getId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
