package ru.homyakin.seeker.telegram.command.common.world_raid;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public record WorldRaidDonate(
    long chatId,
    boolean isPrivate,
    UserId userId
) implements Command {
    public static WorldRaidDonate from(Message message) {
        return new WorldRaidDonate(
            message.getChatId(),
            !TelegramUtils.isGroupMessage(message),
            UserId.from(message.getFrom().getId())
        );
    }
}
