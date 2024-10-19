package ru.homyakin.seeker.telegram.command.group.duel;

import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public interface ProcessDuel extends Command {
    String callbackId();

    GroupTgId groupId();

    UserId userId();

    long duelId();

    int messageId();

    String currentText();
}
