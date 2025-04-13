package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record WorldRaidDonate(
    UserId userId
) implements Command {
    public static WorldRaidDonate from(Message message) {
        return new WorldRaidDonate(UserId.from(message.getFrom().getId()));
    }
}
