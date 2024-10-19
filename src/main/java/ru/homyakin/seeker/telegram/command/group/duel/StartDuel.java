package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record StartDuel(
    GroupTgId groupId,
    UserId userId,
    Optional<MentionInfo> mentionInfo
) implements Command {
    public static StartDuel from(Message message) {
        return new StartDuel(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            MentionInfo.from(message)
        );
    }
}
