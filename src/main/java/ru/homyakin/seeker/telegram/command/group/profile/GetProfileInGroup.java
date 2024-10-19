package ru.homyakin.seeker.telegram.command.group.profile;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetProfileInGroup(GroupTgId groupId, UserId userId) implements Command {
    public static GetProfileInGroup from(Message message) {
        return new GetProfileInGroup(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
