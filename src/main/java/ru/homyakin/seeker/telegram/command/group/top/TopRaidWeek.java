package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopRaidWeek(
    UserId userId,
    GroupId groupId
) implements Command {
    public static TopRaidWeek from(Message message) {
        return new TopRaidWeek(
            UserId.from(message.getFrom().getId()),
            GroupId.from(message.getChatId())
        );
    }
}
