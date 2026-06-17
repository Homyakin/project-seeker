package ru.homyakin.seeker.telegram.command.group.top;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopOutpostBuildSession(
    GroupTgId groupTgId,
    UserId userId,
    Optional<Parsed> parsed
) implements UserGroupCommand {

    public record Parsed(Building building, int targetLevel) {
    }

    public static TopOutpostBuildSession from(Message message) {
        final var text = message.getText().split("@")[0]
            .replace(CommandType.TOP_OUTPOST_BUILD_SESSION.getText(), "")
            .trim();
        final var split = text.split(TextConstants.TG_COMMAND_DELIMITER);
        Optional<Parsed> parsed = Optional.empty();
        if (split.length == 3) {
            try {
                parsed = Optional.of(new Parsed(
                    Building.fromId(Integer.parseInt(split[1])),
                    Integer.parseInt(split[2])
                ));
            } catch (NumberFormatException e) {
                parsed = Optional.empty();
            }
        }
        return new TopOutpostBuildSession(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            parsed
        );
    }
}
