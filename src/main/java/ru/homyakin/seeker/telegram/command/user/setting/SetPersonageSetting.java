package ru.homyakin.seeker.telegram.command.user.setting;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SetPersonageSetting(
    UserId userId,
    int messageId,
    PersonageSetting setting,
    boolean value
) implements Command {
    public static SetPersonageSetting from(CallbackQuery callback) {
        final var parsed = PersonageSettingsCallbackUtils.parseCallback(callback.getData());
        return new SetPersonageSetting(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            parsed.first(),
            parsed.second()
        );
    }
}
