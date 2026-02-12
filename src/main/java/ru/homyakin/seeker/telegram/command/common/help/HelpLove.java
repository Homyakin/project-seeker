package ru.homyakin.seeker.telegram.command.common.help;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public record HelpLove(
    long chatId,
    boolean isPrivate
) implements Command {
    public static HelpLove from(Message message) {
        return new HelpLove(message.getChatId(), !TelegramUtils.isGroupMessage(message));
    }
}
