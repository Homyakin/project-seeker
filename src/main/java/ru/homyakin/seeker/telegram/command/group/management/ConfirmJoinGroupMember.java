package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmJoinGroupMember(
    String callbackId,
    GroupTgId groupTgId,
    UserId userId,
    int messageId,
    PersonageId personageId
) implements Command {
    public static ConfirmJoinGroupMember from(CallbackQuery callback) {
        return new ConfirmJoinGroupMember(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            PersonageId.from(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
