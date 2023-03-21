package ru.homyakin.seeker.telegram.command.user.language;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.Command;

public record UserSelectLanguage(
    String callbackId,
    Long userId,
    Integer messageId,
    Language language
) implements Command {
    public static UserSelectLanguage from(CallbackQuery callback) {
        return new UserSelectLanguage(
            callback.getId(),
            callback.getFrom().getId(),
            callback.getMessage().getMessageId(),
            Language.getOrDefault(Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]))
        );
    }
}
