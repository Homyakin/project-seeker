package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public record Order(
    long groupId,
    long userId,
    int messageId,
    Optional<Integer> itemId
) implements Command {
    public static Order from(Message message) {
        Optional<Integer> itemId;
        try {
            itemId = Optional.of(
                Integer.parseInt(message.getText().split("@")[0].replace(CommandType.ORDER.getText(), ""))
            );
        } catch (NumberFormatException e) {
            itemId = Optional.empty();
        }
        return new Order(
            message.getChatId(),
            message.getFrom().getId(),
            message.getMessageId(),
            itemId
        );
    }
}
