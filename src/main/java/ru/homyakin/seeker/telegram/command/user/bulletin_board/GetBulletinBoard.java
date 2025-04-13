package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record GetBulletinBoard(
    UserId userId
) implements Command {
    public static GetBulletinBoard from(Message message) {
        return new GetBulletinBoard(UserId.from(message.getFrom().getId()));
    }
}
