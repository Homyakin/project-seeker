package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupTax(
    GroupTgId groupTgId
) implements Command {
    public static GroupTax from(Message message) {
        return new GroupTax(
            GroupTgId.from(message.getChatId())
        );
    }
}
