package ru.homyakin.seeker.telegram.command.group.stats;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetPersonageStats(
    GroupTgId groupId,
    UserId userId,
    int messageId
) implements Command {
    public static GetPersonageStats from(Message message) {
        return new GetPersonageStats(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            message.getMessageId()
        );
    }
}
