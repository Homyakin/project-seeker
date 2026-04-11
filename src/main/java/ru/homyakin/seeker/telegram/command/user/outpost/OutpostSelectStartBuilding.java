package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OutpostSelectStartBuilding(
    UserId userId,
    int messageId,
    String callbackId,
    Building building
) implements UserCommand {
    public static OutpostSelectStartBuilding from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new OutpostSelectStartBuilding(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            Building.fromId(Integer.parseInt(parts[1]))
        );
    }
}
