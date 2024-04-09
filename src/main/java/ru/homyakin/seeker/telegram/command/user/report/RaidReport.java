package ru.homyakin.seeker.telegram.command.user.report;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record RaidReport(
    UserId userId
) implements Command {
    public static RaidReport from(Message message) {
        return new RaidReport(UserId.from(message.getFrom().getId()));
    }
}
