package ru.homyakin.seeker.telegram.command.user.stats;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record PersonageStatsGlobal(
    UserId userId
) implements Command {
    public static PersonageStatsGlobal from(Message message) {
        return new PersonageStatsGlobal(UserId.from(message.getFrom().getId()));
    }
}
