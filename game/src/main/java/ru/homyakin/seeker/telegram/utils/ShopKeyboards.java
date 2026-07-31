package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public final class ShopKeyboards {
    private ShopKeyboards() {
    }

    public static InlineKeyboardMarkup navigationKeyboard(Language language) {
        return navigationKeyboard(language, null, 0, 1);
    }

    public static InlineKeyboardMarkup navigationKeyboard(
        Language language,
        CommandType pageCommand,
        int page,
        int totalPages
    ) {
        final var builder = InlineKeyboardBuilder.builder()
            .addRow()
            .addButton(
                ShopLocalization.randomBoxesButton(language),
                CommandType.SHOP_RANDOM_BOXES.getText()
            )
            .addButton(
                MenuLocalization.enhanceButton(language),
                CommandType.SHOP_ENHANCE_INLINE.getText()
            );
        final var slots = PersonageSlot.values();
        for (int i = 0; i < slots.length; ++i) {
            if (i % 4 == 0) {
                builder.addRow();
            }
            final var slot = slots[i];
            builder.addButton(
                slot.icon,
                CommandType.SHOP_SELECT_SLOT.getText() + TextConstants.CALLBACK_DELIMITER + slot.id
            );
        }
        if (pageCommand != null) {
            final var pageCallbackPrefix = pageCommand.getText() + TextConstants.CALLBACK_DELIMITER;
            builder.addPaginationRow(page, totalPages, p -> pageCallbackPrefix + p);
        }
        return builder.build();
    }
}
