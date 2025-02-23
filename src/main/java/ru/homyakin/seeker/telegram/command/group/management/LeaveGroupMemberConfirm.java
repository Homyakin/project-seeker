package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LeaveGroupMemberConfirm(
    GroupTgId groupTgId,
    UserId userId,
    String callbackId,
    int messageId,
    PersonageId personageId
) implements Command {
    public static LeaveGroupMemberConfirm from(CallbackQuery callback) {
        return new LeaveGroupMemberConfirm(
            GroupTgId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getId(),
            callback.getMessage().getMessageId(),
            PersonageId.from(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
