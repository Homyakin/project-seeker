package ru.homyakin.seeker.telegram.command.common.help;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public record SelectHelp(
    long chatId,
    int messageId,
    boolean isPrivate,
    String helpSection,
    int skillsPage,
    Optional<PersonageSlot> skillsSlotFilter
) implements Command {
    private static final String ALL_SLOTS = "ALL";

    public static SelectHelp from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        final var section = parts[1];
        var page = 0;
        Optional<PersonageSlot> slotFilter = Optional.empty();
        if (parts.length >= 3) {
            page = Integer.parseInt(parts[2]);
        }
        if (parts.length >= 4 && !ALL_SLOTS.equals(parts[3])) {
            slotFilter = Optional.of(PersonageSlot.valueOf(parts[3]));
        }
        return new SelectHelp(
            callback.getMessage().getChatId(),
            callback.getMessage().getMessageId(),
            !TelegramUtils.isGroupMessage(callback.getMessage()),
            section,
            page,
            slotFilter
        );
    }
}
