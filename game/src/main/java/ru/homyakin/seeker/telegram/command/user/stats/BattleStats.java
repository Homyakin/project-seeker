package ru.homyakin.seeker.telegram.command.user.stats;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record BattleStats(
    UserId userId,
    Optional<Integer> messageId
) implements UserCommand {
    public static BattleStats from(Message message) {
        return new BattleStats(UserId.from(message.getFrom().getId()), Optional.empty());
    }

    public static BattleStats from(CallbackQuery callback) {
        return new BattleStats(
            UserId.from(callback.getFrom().getId()),
            Optional.of(callback.getMessage().getMessageId())
        );
    }
}
