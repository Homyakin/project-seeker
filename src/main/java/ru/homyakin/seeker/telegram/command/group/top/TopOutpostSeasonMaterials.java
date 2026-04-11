package ru.homyakin.seeker.telegram.command.group.top;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopOutpostSeasonMaterials(
    GroupTgId groupTgId,
    UserId userId
) implements UserGroupCommand {
    public static TopOutpostSeasonMaterials from(Message message) {
        return new TopOutpostSeasonMaterials(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId())
        );
    }
}
