package ru.homyakin.seeker.telegram.command.group.language;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GroupSelectLanguage(
    String callbackId,
    GroupTgId groupTgId,
    Integer messageId,
    UserId userId,
    Language language
) implements UserGroupCommand {
    public static GroupSelectLanguage from(CallbackQuery callback) {
        return new GroupSelectLanguage(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Language.getOrDefault(Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]))
        );
    }
}
