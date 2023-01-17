package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Optional;
import ru.homyakin.seeker.telegram.command.Command;

public record StartDuel(
    long groupId,
    long userId,
    Optional<ReplyInfo> replyInfo
) implements Command {
    public record ReplyInfo(
        int messageId,
        long userId,
        boolean isBot
    ) {
    }
}
