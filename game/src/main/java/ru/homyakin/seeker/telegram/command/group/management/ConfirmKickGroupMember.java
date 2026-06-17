package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmKickGroupMember(
    GroupTgId groupTgId,
    UserId userId,
    String callbackId,
    int messageId,
    PersonageId targetPersonageId
) implements UserGroupCommand {
    public static ConfirmKickGroupMember from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new ConfirmKickGroupMember(
            GroupTgId.from(callback.getMessage().getChatId()),
            UserId.from(callback.getFrom().getId()),
            callback.getId(),
            callback.getMessage().getMessageId(),
            PersonageId.from(parts[1])
        );
    }
}

