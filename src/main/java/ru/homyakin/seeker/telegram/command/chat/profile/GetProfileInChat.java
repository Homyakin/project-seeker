package ru.homyakin.seeker.telegram.command.chat.profile;

import ru.homyakin.seeker.telegram.command.Command;

public record GetProfileInChat(
    Long chatId,
    Long userId
) implements Command {
}
