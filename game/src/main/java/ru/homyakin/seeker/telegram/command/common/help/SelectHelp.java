package ru.homyakin.seeker.telegram.command.common.help;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    Set<PersonageSlot> skillsSlotFilters
) implements Command {
    static final String EMPTY_SLOT_FILTERS = "-";
    private static final String SLOT_IDS_DELIMITER = ",";

    public static SelectHelp from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER, -1);
        final var section = parts[1];
        var page = 0;
        Set<PersonageSlot> slotFilters = Set.of();
        if (parts.length >= 3 && !parts[2].isEmpty()) {
            page = Integer.parseInt(parts[2]);
        }
        if (parts.length >= 4) {
            slotFilters = parseSlotFilters(parts[3]);
        }
        return new SelectHelp(
            callback.getMessage().getChatId(),
            callback.getMessage().getMessageId(),
            !TelegramUtils.isGroupMessage(callback.getMessage()),
            section,
            page,
            slotFilters
        );
    }

    public static Set<PersonageSlot> parseSlotFilters(String encoded) {
        if (encoded == null || encoded.isEmpty() || EMPTY_SLOT_FILTERS.equals(encoded)) {
            return Set.of();
        }
        final var filters = EnumSet.noneOf(PersonageSlot.class);
        for (final var part : encoded.split(SLOT_IDS_DELIMITER)) {
            if (!part.isEmpty()) {
                filters.add(PersonageSlot.findById(Integer.parseInt(part)));
            }
        }
        return Set.copyOf(filters);
    }

    public static String encodeSlotFilters(Set<PersonageSlot> filters) {
        if (filters.isEmpty()) {
            return EMPTY_SLOT_FILTERS;
        }
        return filters.stream()
            .sorted((a, b) -> Integer.compare(a.id, b.id))
            .map(slot -> String.valueOf(slot.id))
            .collect(Collectors.joining(SLOT_IDS_DELIMITER));
    }
}
