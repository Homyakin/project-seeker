package ru.homyakin.seeker.telegram.command.group.trigger;

import io.vavr.control.Either;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.models.GroupId;

import java.util.Optional;

public record AddTrigger(
        GroupId groupId,
        String textToTrigger,
        Either<TriggerCommandError, String> triggerText
) implements Command {

    public static AddTrigger from(Message message) {
        final var textToTrigger = message.getText()
                .replaceAll(CommandType.ADD_TRIGGER.getText(), "")
                .trim();

        Optional<String> optionalTriggerText = Optional.ofNullable(message.getReplyToMessage()).map(Message::getText);

        Either<TriggerCommandError, String> triggerText =
                optionalTriggerText.<Either<TriggerCommandError, String>>map(Either::right)
                .orElseGet(
                        () -> Either.left(TriggerCommandError.NoTriggerTextCommandError.INSTANCE)
                );

        return new AddTrigger(
                GroupId.from(message.getChatId()),
                textToTrigger,
                triggerText
        );
    }
}
