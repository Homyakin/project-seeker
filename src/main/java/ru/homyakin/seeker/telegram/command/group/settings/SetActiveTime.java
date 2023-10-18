package ru.homyakin.seeker.telegram.command.group.settings;

import io.vavr.control.Either;
import java.util.Arrays;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SetActiveTime(
    long groupId,
    UserId userId,
    Either<ActiveTimeCommandError, Info> info
) implements Command {
    public static SetActiveTime from(Message message) {
        final var data = message.getText()
            .replaceAll(CommandType.SET_ACTIVE_TIME.getText(), "")
            .trim()
            .split(" ");
        return new SetActiveTime(
            message.getChatId(),
            UserId.from(message.getFrom().getId()),
            Info.from(data)
        );
    }

    record Info(
        int startHour,
        int endHour,
        int timeZone
    ) {
        public static Either<ActiveTimeCommandError, Info> from(String[] data) {
            if (data.length != 3) {
                return Either.left(ActiveTimeCommandError.IncorrectArgumentsNumber.INSTANCE);
            }
            try {
                final var numbers = Arrays.stream(data).map(Integer::parseInt).toList();
                return Either.right(
                    new Info(
                        numbers.get(0),
                        numbers.get(1),
                        numbers.get(2)
                    )
                );
            } catch (NumberFormatException e) {
                return Either.left(ActiveTimeCommandError.ArgumentsNotANumber.INSTANCE);
            }
        }
    }
}
