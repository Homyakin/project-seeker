package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;
import ru.homyakin.seeker.utils.CommonUtils;

import java.util.Optional;

public record TakePersonalQuest(
    UserId userId,
    Optional<Integer> count
) implements Command {
    public static TakePersonalQuest from(Message message, boolean isCommand) {
        final Optional<Integer> count;
        if (isCommand) {
            count = TelegramUtils.deleteCommand(message.getText()).flatMap(CommonUtils::parseIntOrEmpty);
        } else {
            count = Optional.of(1);
        }
        return new TakePersonalQuest(UserId.from(message.getFrom().getId()), count);
    }
}
