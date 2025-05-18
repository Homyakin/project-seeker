package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenEnhanceTable(UserId userId) implements Command {
    public static OpenEnhanceTable from(Message message) {
        return new OpenEnhanceTable(UserId.from(message.getFrom().getId()));
    }
}
