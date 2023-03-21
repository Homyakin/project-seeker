package ru.homyakin.seeker.telegram.command.common.help;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public record SelectHelp(
    long chatId,
    int messageId,
    boolean isPrivate,
    String helpSection
) implements Command {
    public static SelectHelp from(CallbackQuery callback) {
        return new SelectHelp(
            callback.getMessage().getChatId(),
            callback.getMessage().getMessageId(),
            !TelegramUtils.isGroupMessage(callback.getMessage()),
            callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]
        );
    }
}
