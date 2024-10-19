package ru.homyakin.seeker.telegram.command.group.language;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupChangeLanguage(GroupTgId groupId) implements Command {
    public static GroupChangeLanguage from(Message message) {
        return new GroupChangeLanguage(GroupTgId.from(message.getChatId()));
    }
}
