package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record AnomalyStart(
    String callbackId,
    GroupTgId groupTgId,
    Integer messageId,
    UserId userId
) implements UserGroupCommand {
    public static AnomalyStart from(CallbackQuery callback) {
        return new AnomalyStart(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId())
        );
    }
}
