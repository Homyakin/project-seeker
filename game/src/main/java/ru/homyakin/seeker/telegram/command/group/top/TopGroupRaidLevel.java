package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record TopGroupRaidLevel(
    GroupTgId groupTgId
) implements GroupCommand {
    public static TopGroupRaidLevel from(Message message) {
        return new TopGroupRaidLevel(
            GroupTgId.from(message.getChatId())
        );
    }
}
