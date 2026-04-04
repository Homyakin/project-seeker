package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OutpostBuildingContributeView(
    UserId userId,
    int messageId,
    String callbackId,
    Building building
) implements Command {

    public static OutpostBuildingContributeView from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new OutpostBuildingContributeView(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            Building.fromId(Integer.parseInt(parts[1]))
        );
    }
}
