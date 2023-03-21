package ru.homyakin.seeker.telegram.command.group.profile;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record GetProfileInGroup(Long groupId, Long userId) implements Command {
    public static GetProfileInGroup from(Message message) {
        return new GetProfileInGroup(message.getChatId(), message.getFrom().getId());
    }
}
