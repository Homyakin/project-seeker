package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;
import ru.homyakin.seeker.utils.CommonUtils;

import java.util.Optional;

public record GiveMoneyFromGroup(
    GroupTgId groupTgId,
    UserId userId,
    Optional<Integer> amount,
    Optional<MentionInfo> mention
) implements Command {
    public static GiveMoneyFromGroup from(Message message) {
        final var amount = TelegramUtils.deleteCommand(message.getText())
            .map(it -> it.split(" "))
            .flatMap(it -> Optional.ofNullable(it.length > 0 ? it[0] : null))
            .flatMap(CommonUtils::parseIntOrEmpty);
        return new GiveMoneyFromGroup(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            amount,
            MentionInfo.from(message)
        );
    }
}
