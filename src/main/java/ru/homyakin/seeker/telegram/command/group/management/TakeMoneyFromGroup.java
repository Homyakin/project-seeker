package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;
import ru.homyakin.seeker.utils.CommonUtils;

import java.util.Optional;

public record TakeMoneyFromGroup(
    GroupTgId groupTgId,
    UserId userId,
    Optional<Integer> amount
) implements Command {
    public static TakeMoneyFromGroup from(Message message) {
        return new TakeMoneyFromGroup(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            TelegramUtils.deleteCommand(message.getText()).flatMap(CommonUtils::parseIntOrEmpty)
        );
    }
}
