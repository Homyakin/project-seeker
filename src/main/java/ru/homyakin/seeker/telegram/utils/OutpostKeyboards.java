package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public final class OutpostKeyboards {
    private OutpostKeyboards() {
    }

    public static InlineKeyboardMarkup openOutpostInPrivateKeyboard(Language language) {
        final var url = "https://t.me/%s?start=%s".formatted(
            TelegramBotConfig.username(),
            CommandType.OPEN_OUTPOST_MENU.getText()
        );
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addUrlButton(OutpostLocalization.openInPrivateButton(language), url)
            .build();
    }
}
