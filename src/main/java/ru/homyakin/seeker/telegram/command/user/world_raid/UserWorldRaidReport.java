package ru.homyakin.seeker.telegram.command.user.world_raid;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record UserWorldRaidReport(
    UserId userId
) implements Command {
    public static UserWorldRaidReport from(Message message) {
        return new UserWorldRaidReport(UserId.from(message.getFrom().getId()));
    }
}
