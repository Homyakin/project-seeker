package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopDonate(
    GroupTgId groupTgId,
    UserId userId
) implements Command {
    public static TopDonate from(Message message) {
        return new TopDonate(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId())
        );
    }
}
