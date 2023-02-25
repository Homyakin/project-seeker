package ru.homyakin.seeker.telegram.command.user.navigation;

import ru.homyakin.seeker.telegram.command.Command;

public record ReceptionDesk(
    long userId
) implements Command {
}
