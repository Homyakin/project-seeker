package ru.homyakin.seeker.telegram.command.group.language;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.Command;

public record GroupSelectLanguage(
    String callbackId,
    Long groupId,
    Integer messageId,
    Long userId,
    Language language
) implements Command {
    public static GroupSelectLanguage from(CallbackQuery callback) {
        return new GroupSelectLanguage(
            callback.getId(),
            callback.getMessage().getChatId(),
            callback.getMessage().getMessageId(),
            callback.getFrom().getId(),
            Language.getOrDefault(Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]))
        );
    }
}
