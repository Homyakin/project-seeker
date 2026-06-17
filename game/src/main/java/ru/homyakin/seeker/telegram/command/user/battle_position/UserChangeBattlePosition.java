package ru.homyakin.seeker.telegram.command.user.battle_position;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserChangeBattlePosition(
    UserId userId
) implements UserCommand {
    public static UserChangeBattlePosition from(Message message) {
        return new UserChangeBattlePosition(UserId.from(message.getFrom().getId()));
    }
}
