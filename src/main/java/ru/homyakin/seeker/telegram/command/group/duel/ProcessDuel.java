package ru.homyakin.seeker.telegram.command.group.duel;

import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public interface ProcessDuel extends Command {
    String callbackId();

    GroupId groupId();

    UserId userId();

    long duelId();

    int messageId();

    String currentText();
}
