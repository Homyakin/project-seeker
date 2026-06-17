package ru.homyakin.seeker.telegram.command.user.stats;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record BattleStats(
    UserId userId
) implements UserCommand {
    public static BattleStats from(Message message) {
        return new BattleStats(UserId.from(message.getFrom().getId()));
    }
}
