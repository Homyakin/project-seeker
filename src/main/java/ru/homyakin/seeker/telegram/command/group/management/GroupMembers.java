package ru.homyakin.seeker.telegram.command.group.management;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupMembers(
    GroupTgId groupTgId,
    int page,
    Optional<String> callbackId,
    Optional<Integer> messageId
) implements GroupCommand {
    public static GroupMembers fromMessage(Message message) {
        return new GroupMembers(
            GroupTgId.from(message.getChatId()),
            1,
            Optional.empty(),
            Optional.empty()
        );
    }

    public static GroupMembers fromCallback(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        int page = 0;
        if (parts.length > 1) {
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException _) {
                page = 0;
            }
        }
        return new GroupMembers(
            GroupTgId.from(callback.getMessage().getChatId()),
            page,
            Optional.of(callback.getId()),
            Optional.of(callback.getMessage().getMessageId())
        );
    }
}
