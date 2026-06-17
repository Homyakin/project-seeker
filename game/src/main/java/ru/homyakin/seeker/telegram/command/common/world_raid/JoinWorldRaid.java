package ru.homyakin.seeker.telegram.command.common.world_raid;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record JoinWorldRaid(
    UserId userId,
    String callbackId
) implements UserCommand {
    public static JoinWorldRaid from(CallbackQuery callback) {
        return new JoinWorldRaid(
            UserId.from(callback.getFrom().getId()),
            callback.getId()
        );
    }
}
