package ru.homyakin.seeker.telegram.command.group.language;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GroupSelectLanguage(
    String callbackId,
    GroupId groupId,
    Integer messageId,
    UserId userId,
    Language language
) implements Command {
    public static GroupSelectLanguage from(CallbackQuery callback) {
        return new GroupSelectLanguage(
            callback.getId(),
            GroupId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Language.getOrDefault(Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]))
        );
    }
}
