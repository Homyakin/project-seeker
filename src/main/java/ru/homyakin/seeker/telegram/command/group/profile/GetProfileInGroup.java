package ru.homyakin.seeker.telegram.command.group.profile;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetProfileInGroup(GroupId groupId, UserId userId) implements Command {
    public static GetProfileInGroup from(Message message) {
        return new GetProfileInGroup(GroupId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
