package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record ConsumeOrder(
    String callbackId,
    Long groupId,
    Integer messageId,
    Long userId,
    Long orderId
) implements Command {
    public static ConsumeOrder from(CallbackQuery callback) {
        return new ConsumeOrder(
            callback.getId(),
            callback.getMessage().getChatId(),
            callback.getMessage().getMessageId(),
            callback.getFrom().getId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
