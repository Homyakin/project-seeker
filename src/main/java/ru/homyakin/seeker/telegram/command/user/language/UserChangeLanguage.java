package ru.homyakin.seeker.telegram.command.user.language;

import ru.homyakin.seeker.telegram.command.Command;

public record UserChangeLanguage(
    Long userId
) implements Command {
}
