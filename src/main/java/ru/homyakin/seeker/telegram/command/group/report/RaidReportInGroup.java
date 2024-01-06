package ru.homyakin.seeker.telegram.command.group.report;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record RaidReportInGroup(
    GroupId groupId,
    UserId userId,
    int messageId
) implements Command {
    public static RaidReportInGroup from(Message message) {
        return new RaidReportInGroup(
            GroupId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            message.getMessageId()
        );
    }
}
