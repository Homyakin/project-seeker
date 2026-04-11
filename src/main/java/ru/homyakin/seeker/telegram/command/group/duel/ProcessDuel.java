package ru.homyakin.seeker.telegram.command.group.duel;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public interface ProcessDuel extends UserGroupCommand {
    String callbackId();

    GroupTgId groupTgId();

    UserId userId();

    long duelId();

    int messageId();

    String currentText();
}
