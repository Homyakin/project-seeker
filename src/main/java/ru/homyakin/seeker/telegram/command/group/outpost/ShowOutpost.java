package ru.homyakin.seeker.telegram.command.group.outpost;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record ShowOutpost(GroupTgId groupId) implements Command {
    public static ShowOutpost from(Message message) {
        return new ShowOutpost(GroupTgId.from(message.getChatId()));
    }
}
