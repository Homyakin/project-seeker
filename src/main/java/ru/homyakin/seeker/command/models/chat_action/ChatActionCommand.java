package ru.homyakin.seeker.command.models.chat_action;

import ru.homyakin.seeker.command.models.Command;

public interface ChatActionCommand extends Command {
    Long chatId();
}


