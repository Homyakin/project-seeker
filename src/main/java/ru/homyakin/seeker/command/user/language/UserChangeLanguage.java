package ru.homyakin.seeker.command.user.language;

import ru.homyakin.seeker.command.Command;

public record UserChangeLanguage(
    Long userId
) implements Command {
}
