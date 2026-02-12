package ru.homyakin.seeker.telegram.command.group.valentine;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public record SendValentine(
    GroupTgId groupId,
    UserId userId,
    Optional<String> tag,
    Optional<MentionInfo> mentionInfo
) implements Command {

    public static SendValentine from(Message message) {
        final var tag = TelegramUtils.deleteCommand(message.getText())
            .map(text -> text.split("\\s+")[0]);
        return new SendValentine(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            tag,
            MentionInfo.from(message)
        );
    }
}
