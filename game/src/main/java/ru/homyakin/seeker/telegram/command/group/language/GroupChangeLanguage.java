package ru.homyakin.seeker.telegram.command.group.language;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupChangeLanguage(GroupTgId groupTgId) implements GroupCommand {
    public static GroupChangeLanguage from(Message message) {
        return new GroupChangeLanguage(GroupTgId.from(message.getChatId()));
    }
}
