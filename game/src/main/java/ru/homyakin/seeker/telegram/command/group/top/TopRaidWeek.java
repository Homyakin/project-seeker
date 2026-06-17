package ru.homyakin.seeker.telegram.command.group.top;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopRaidWeek(
    UserId userId,
    GroupTgId groupTgId
) implements UserGroupCommand {
    public static TopRaidWeek from(Message message) {
        return new TopRaidWeek(
            UserId.from(message.getFrom().getId()),
            GroupTgId.from(message.getChatId())
        );
    }
}
