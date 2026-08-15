package ru.homyakin.seeker.telegram.command.user.targeting_tactic;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserChangeTargetingTactic(
    UserId userId
) implements UserCommand {
    public static UserChangeTargetingTactic from(Message message) {
        return new UserChangeTargetingTactic(UserId.from(message.getFrom().getId()));
    }
}
