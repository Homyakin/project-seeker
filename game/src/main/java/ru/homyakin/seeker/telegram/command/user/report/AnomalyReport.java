package ru.homyakin.seeker.telegram.command.user.report;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record AnomalyReport(
    UserId userId
) implements UserCommand {
    public static AnomalyReport from(Message message) {
        return new AnomalyReport(UserId.from(message.getFrom().getId()));
    }
}
