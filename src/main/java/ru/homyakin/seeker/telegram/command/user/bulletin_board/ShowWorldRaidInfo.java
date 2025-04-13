package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ShowWorldRaidInfo(
    UserId userId
) implements Command {
    public static ShowWorldRaidInfo from(Message message) {
        return new ShowWorldRaidInfo(UserId.from(message.getFrom().getId()));
    }
}
