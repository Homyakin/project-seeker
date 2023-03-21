package ru.homyakin.seeker.telegram.command.group.language;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record GroupChangeLanguage(Long groupId) implements Command {
    public static GroupChangeLanguage from(Message message) {
        return new GroupChangeLanguage(message.getChatId());
    }
}
