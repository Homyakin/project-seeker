package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record Inventory(
    UserId userId
) implements Command {
    public static Inventory from(Message message) {
        return new Inventory(
            UserId.from(message.getFrom().getId())
        );
    }
}
