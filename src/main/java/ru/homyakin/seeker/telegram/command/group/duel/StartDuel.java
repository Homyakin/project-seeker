package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Optional;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.models.ReplyInfo;

public record StartDuel(
    long groupId,
    long userId,
    Optional<ReplyInfo> replyInfo
) implements Command {
}
