package ru.homyakin.seeker.telegram.command.user.targeting_tactic;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserSelectTargetingTactic(
    String callbackId,
    UserId userId,
    Integer messageId,
    TargetingTactic targetingTactic
) implements UserCommand {
    public static UserSelectTargetingTactic from(CallbackQuery callback) {
        return new UserSelectTargetingTactic(
            callback.getId(),
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            TargetingTactic.fromString(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
