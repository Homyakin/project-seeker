package ru.homyakin.seeker.telegram.command;

import ru.homyakin.seeker.telegram.user.models.UserId;

public interface UserCommand extends Command {
    UserId userId();
}
