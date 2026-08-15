package ru.homyakin.seeker.telegram.command.user.battle_position;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserChangeBattlePosition(
    UserId userId,
    Optional<Integer> messageId
) implements UserCommand {
    public static UserChangeBattlePosition from(Message message) {
        return new UserChangeBattlePosition(UserId.from(message.getFrom().getId()), Optional.empty());
    }

    public static UserChangeBattlePosition from(CallbackQuery callback) {
        return new UserChangeBattlePosition(
            UserId.from(callback.getFrom().getId()),
            Optional.of(callback.getMessage().getMessageId())
        );
    }
}
