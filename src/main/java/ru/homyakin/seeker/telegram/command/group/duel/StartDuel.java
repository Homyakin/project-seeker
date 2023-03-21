package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.models.MessageOwner;
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
            Optional.ofNullable(message.getReplyToMessage()).map(
                it -> new ReplyInfo(it.getMessageId(), it.getFrom().getId(), MessageOwner.from(it))
            )
        );
    }
}
