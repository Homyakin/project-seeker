package ru.homyakin.seeker.telegram.command.user.outpost;

import java.util.Optional;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OutpostDonateItem(UserId userId, Building building, long itemId) implements UserCommand {

    public static Optional<OutpostDonateItem> tryParse(Message message) {
        final var raw = message.getText().split("@")[0].trim();
        final var prefix = CommandType.OUTPOST_DONATE_ITEM.getText() + TextConstants.TG_COMMAND_DELIMITER;
        if (!raw.startsWith(prefix)) {
            return Optional.empty();
        }
        final var rest = raw.substring(prefix.length());
        final var lastUs = rest.lastIndexOf(TextConstants.TG_COMMAND_DELIMITER);
        if (lastUs < 0) {
            return Optional.empty();
        }
        try {
            final var buildingId = Integer.parseInt(rest.substring(0, lastUs));
            final var itemId = Long.parseLong(rest.substring(lastUs + 1));
            return Optional.of(new OutpostDonateItem(
                UserId.from(message.getFrom().getId()),
                Building.fromId(buildingId),
                itemId
            ));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
