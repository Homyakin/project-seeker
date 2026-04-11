package ru.homyakin.seeker.telegram.command.group.profile;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetProfileInGroup(GroupTgId groupTgId, UserId userId) implements UserGroupCommand {
    public static GetProfileInGroup from(Message message) {
        return new GetProfileInGroup(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
