package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.models.ReplyInfo;

public record StartDuel(
    long groupId,
    long userId,
    Optional<ReplyInfo> replyInfo
) implements Command {
    public static StartDuel from(Message message) {
        return new StartDuel(
            message.getChatId(),
            message.getFrom().getId(),
            ReplyInfo.from(message)
        );
    }
}
