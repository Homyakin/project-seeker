package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.CommonUtils;

public record OutpostBuildingContributeView(
    UserId userId,
    int messageId,
    String callbackId,
    Building building,
    int page
) implements UserCommand {

    public static OutpostBuildingContributeView from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        final var page = parts.length > 2
            ? CommonUtils.parseIntOrEmpty(parts[2]).orElse(0)
            : 0;
        return new OutpostBuildingContributeView(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            Building.fromId(Integer.parseInt(parts[1])),
            page
        );
    }
}
