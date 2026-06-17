package ru.homyakin.seeker.telegram.command.group.management;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupMember(
    GroupTgId groupTgId,
    PersonageId personageId
) implements GroupCommand {
    public static Optional<GroupMember> tryParse(Message message) {
        try {
            final var token = message.getText().split("@")[0].split(" ")[0];
            final var parts = token.split(TextConstants.TG_COMMAND_DELIMITER, 2);
            if (parts.length < 2) {
                return Optional.empty();
            }
            final var id = Long.parseLong(parts[1]);
            return Optional.of(new GroupMember(GroupTgId.from(message.getChatId()), PersonageId.from(id)));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }
}
