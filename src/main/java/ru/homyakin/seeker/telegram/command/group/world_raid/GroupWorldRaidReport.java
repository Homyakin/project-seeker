package ru.homyakin.seeker.telegram.command.group.world_raid;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupWorldRaidReport(
    GroupTgId groupTgId,
    int messageId
) implements Command {
    public static GroupWorldRaidReport from(Message message) {
        return new GroupWorldRaidReport(GroupTgId.from(message.getChatId()), message.getMessageId());
    }
}
