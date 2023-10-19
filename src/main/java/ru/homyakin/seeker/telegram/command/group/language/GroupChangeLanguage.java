package ru.homyakin.seeker.telegram.command.group.language;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GroupChangeLanguage(GroupId groupId) implements Command {
    public static GroupChangeLanguage from(Message message) {
        return new GroupChangeLanguage(GroupId.from(message.getChatId()));
    }
}
