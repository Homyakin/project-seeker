package ru.homyakin.seeker.telegram.command;

import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public interface GroupCommand extends Command {
    GroupTgId groupTgId();
}
