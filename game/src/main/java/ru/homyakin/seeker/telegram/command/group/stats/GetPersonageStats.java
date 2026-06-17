package ru.homyakin.seeker.telegram.command.group.stats;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetPersonageStats(
    GroupTgId groupTgId,
    UserId userId,
    int messageId
) implements UserGroupCommand {
    public static GetPersonageStats from(Message message) {
        return new GetPersonageStats(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            message.getMessageId()
        );
    }
}
