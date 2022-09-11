package ru.homyakin.seeker.command.user.language;

import ru.homyakin.seeker.command.Command;

public record UserSelectLanguage(
    String callbackId,
    Long userId,
    Integer messageId,
    String data
) implements Command {
}
