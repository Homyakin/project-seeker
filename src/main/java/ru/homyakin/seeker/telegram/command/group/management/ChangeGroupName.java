package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public record ChangeGroupName(
    GroupTgId groupTgId,
    UserId userId,
    Optional<String> name
) implements Command {
    public static ChangeGroupName from(Message message) {
        final var splitted = message.getText().split(" ", 2);
        final var name = splitted.length == 2 ? splitted[1] : null;
        return new ChangeGroupName(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            Optional.ofNullable(name)
        );
    }
}
