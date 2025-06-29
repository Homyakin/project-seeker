package ru.homyakin.seeker.telegram.command.group.badge;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record ShowGroupBadges(
    GroupTgId groupTgId
) implements Command {
    public static ShowGroupBadges from(Message message) {
        return new ShowGroupBadges(
            GroupTgId.from(message.getChatId())
        );
    }
}
