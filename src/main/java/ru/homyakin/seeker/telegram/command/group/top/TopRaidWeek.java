package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopRaidWeek(
    UserId userId,
    GroupTgId groupId
) implements Command {
    public static TopRaidWeek from(Message message) {
        return new TopRaidWeek(
            UserId.from(message.getFrom().getId()),
            GroupTgId.from(message.getChatId())
        );
    }
}
