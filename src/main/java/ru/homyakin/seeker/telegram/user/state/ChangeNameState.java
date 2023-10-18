package ru.homyakin.seeker.telegram.user.state;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.ChangeNameCommandType;
import ru.homyakin.seeker.telegram.command.user.change_name.CancelChangeName;
import ru.homyakin.seeker.telegram.command.user.change_name.InputName;

public sealed interface ChangeNameState extends UserState {
    record InitChangeName() implements ChangeNameState {
        @Override
        public Command nextCommand(Message message) {
            return ChangeNameCommandType
                .getFromString(message.getText())
                .map(command -> (Command) switch (command) {
                    case CANCEL -> CancelChangeName.from(message);
                    default -> null;
                })
                .orElseGet(() -> InputName.from(message));
        }
    }

    record ConfirmChangeName(String name) implements ChangeNameState {
        @Override
        public Command nextCommand(Message message) {
            return ChangeNameCommandType
                .getFromString(message.getText())
                .map(command -> switch (command) {
                    case CONFIRM -> ru.homyakin.seeker.telegram.command.user.change_name.ConfirmChangeName.from(message, name);
                    case REPEAT -> ru.homyakin.seeker.telegram.command.user.change_name.InitChangeName.from(message);
                    case CANCEL -> CancelChangeName.from(message);
                })
                .orElseGet(() -> InputName.from(message));
        }
    }
}
