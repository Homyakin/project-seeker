package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenOutpostBuilding(UserId userId, Building building) implements Command {
    public static OpenOutpostBuilding from(Message message) {
        return new OpenOutpostBuilding(
            UserId.from(message.getFrom().getId()),
            Building.fromId(Integer.parseInt(message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1]))
        );
    }
}
