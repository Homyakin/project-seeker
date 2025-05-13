package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

import java.util.Optional;

public record ChangeGroupTag(
    GroupTgId groupTgId,
    UserId userId,
    Optional<String> tag
) implements Command {
    public static ChangeGroupTag from(Message message) {
        return new ChangeGroupTag(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            TelegramUtils.deleteCommand(message.getText()).map(String::trim)
        );
    }
}
