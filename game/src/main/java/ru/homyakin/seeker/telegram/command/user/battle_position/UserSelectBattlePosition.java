package ru.homyakin.seeker.telegram.command.user.battle_position;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserSelectBattlePosition(
    String callbackId,
    UserId userId,
    Integer messageId,
    Position position
) implements UserCommand {
    public static UserSelectBattlePosition from(CallbackQuery callback) {
        return new UserSelectBattlePosition(
            callback.getId(),
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            Position.fromString(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
