package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record TopGroupRaidWeek(
    GroupTgId groupId
) implements Command {
    public static TopGroupRaidWeek from(Message message) {
        return new TopGroupRaidWeek(
            GroupTgId.from(message.getChatId())
        );
    }
}
