package ru.homyakin.seeker.telegram.command.user.outpost;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenOutpostBuilding(
    UserId userId,
    Building building,
    Optional<Integer> editMessageId
) implements UserCommand {

    public static OpenOutpostBuilding from(Message message) {
        return new OpenOutpostBuilding(
            UserId.from(message.getFrom().getId()),
            Building.fromId(Integer.parseInt(message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1])),
            Optional.empty()
        );
    }

    public static OpenOutpostBuilding fromInlineCallback(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new OpenOutpostBuilding(
            UserId.from(callback.getFrom().getId()),
            Building.fromId(Integer.parseInt(parts[1])),
            Optional.of(callback.getMessage().getMessageId())
        );
    }
}
