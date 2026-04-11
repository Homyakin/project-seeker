package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record WorldRaidResearchTop(
    UserId userId
) implements UserCommand {
    public static WorldRaidResearchTop from(Message message) {
        return new WorldRaidResearchTop(UserId.from(message.getFrom().getId()));
    }
}
