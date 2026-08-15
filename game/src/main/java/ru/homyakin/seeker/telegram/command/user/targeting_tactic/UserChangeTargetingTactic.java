package ru.homyakin.seeker.telegram.command.user.targeting_tactic;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserChangeTargetingTactic(
    UserId userId,
    Optional<Integer> messageId
) implements UserCommand {
    public static UserChangeTargetingTactic from(Message message) {
        return new UserChangeTargetingTactic(UserId.from(message.getFrom().getId()), Optional.empty());
    }

    public static UserChangeTargetingTactic from(CallbackQuery callback) {
        return new UserChangeTargetingTactic(
            UserId.from(callback.getFrom().getId()),
            Optional.of(callback.getMessage().getMessageId())
        );
    }
}
