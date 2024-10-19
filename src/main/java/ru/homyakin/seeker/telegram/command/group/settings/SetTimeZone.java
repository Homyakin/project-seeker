package ru.homyakin.seeker.telegram.command.group.settings;

import io.vavr.control.Either;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SetTimeZone(
    GroupTgId groupId,
    UserId userId,
    Either<IncorrectFormat, Integer> data
) implements Command {
    public static SetTimeZone from(Message message) {

        return new SetTimeZone(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            timeZoneFromString(message.getText().split(" "))
        );
    }

    private static Either<IncorrectFormat, Integer> timeZoneFromString(String[] data) {
        if (data.length != 2) {
            return Either.left(IncorrectFormat.INSTANCE);
        }
        try {
            return Either.right(Integer.parseInt(data[1]));
        } catch (NumberFormatException e) {
            return Either.left(IncorrectFormat.INSTANCE);
        }
    }

    enum IncorrectFormat { INSTANCE }
}
