package ru.homyakin.seeker.telegram.command.user.stats;

import ru.homyakin.seeker.telegram.command.Command;

public record ResetStats(long userId) implements Command {
}
