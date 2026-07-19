package ru.homyakin.seeker.telegram.user.state;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.LoadoutNameCommandType;
import ru.homyakin.seeker.telegram.command.user.item.CancelLoadoutName;
import ru.homyakin.seeker.telegram.command.user.item.InputLoadoutName;

public sealed interface LoadoutNameState extends UserState {
    record CreateLoadout(int messageId) implements LoadoutNameState {
        @Override
        public Command nextCommand(Message message) {
            return InputLoadoutName.from(message);
        }
    }

    record RenameLoadout(long loadoutId, int messageId) implements LoadoutNameState {
        @Override
        public Command nextCommand(Message message) {
            return LoadoutNameCommandType
                .getFromString(message.getText())
                .<Command>map(command -> switch (command) {
                    case CANCEL -> CancelLoadoutName.from(message);
                })
                .orElseGet(() -> InputLoadoutName.from(message));
        }
    }
}
