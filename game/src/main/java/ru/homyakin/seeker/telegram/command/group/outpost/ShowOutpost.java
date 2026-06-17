package ru.homyakin.seeker.telegram.command.group.outpost;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record ShowOutpost(GroupTgId groupTgId) implements GroupCommand {
    public static ShowOutpost from(Message message) {
        return new ShowOutpost(GroupTgId.from(message.getChatId()));
    }
}
