package ru.homyakin.seeker.telegram.command.group.action;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record MigrateFromGroup(
    GroupId from,
    GroupId to
) implements Command {
    public static MigrateFromGroup from(Message message) {
        /*
        Во время миграции приходит два апдейта:
        chat = старой группе, MigrateToChatId = новой группе
        И
        chat = новой группе, MigrateFromChatId = старой группе
        обрабатывать два апдейта не имеет смысла
         */
        return new MigrateFromGroup(
            GroupId.from(message.getMigrateFromChatId()),
            GroupId.from(message.getChatId())
        );
    }
}
