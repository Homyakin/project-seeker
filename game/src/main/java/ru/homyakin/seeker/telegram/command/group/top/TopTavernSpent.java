package ru.homyakin.seeker.telegram.command.group.top;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopTavernSpent(
    GroupTgId groupTgId,
    UserId userId
) implements UserGroupCommand {
    public static TopTavernSpent from(Message message) {
        return new TopTavernSpent(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId())
        );
    }
}
