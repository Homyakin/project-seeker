package ru.homyakin.seeker.telegram.command.group.report;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record RaidReportInGroup(
    GroupTgId groupTgId,
    UserId userId,
    int messageId
) implements UserGroupCommand {
    public static RaidReportInGroup from(Message message) {
        return new RaidReportInGroup(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            message.getMessageId()
        );
    }
}
