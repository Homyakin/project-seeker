package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record StartDuel(
    long groupId,
    UserId userId,
    Optional<MentionInfo> mentionInfo
) implements Command {
    public static StartDuel from(Message message) {
        return new StartDuel(
            message.getChatId(),
            UserId.from(message.getFrom().getId()),
            MentionInfo.from(message)
        );
    }
}
