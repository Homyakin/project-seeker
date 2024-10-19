package ru.homyakin.seeker.telegram.command.group.action;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record MigrateFromGroup(
    GroupTgId from,
    GroupTgId to
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
            GroupTgId.from(message.getMigrateFromChatId()),
            GroupTgId.from(message.getChatId())
        );
    }
}
