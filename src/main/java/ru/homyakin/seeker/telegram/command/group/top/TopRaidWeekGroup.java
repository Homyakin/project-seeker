package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopRaidWeekGroup(
    UserId userId,
    GroupId groupId
) implements Command {
    public static TopRaidWeekGroup from(Message message) {
        return new TopRaidWeekGroup(
            UserId.from(message.getFrom().getId()),
            GroupId.from(message.getChatId())
        );
    }
}
