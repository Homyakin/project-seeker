package ru.homyakin.seeker.telegram.command.user.badge;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ShowPersonageBadges(
    UserId userId
) implements UserCommand {
    public static ShowPersonageBadges from(Message message) {
        return new ShowPersonageBadges(
            UserId.from(message.getFrom().getId())
        );
    }
}
