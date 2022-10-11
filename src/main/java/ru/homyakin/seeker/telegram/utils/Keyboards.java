package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.telegram.command.CommandText;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;

public class Keyboards {
    public static InlineKeyboardMarkup languageKeyboard(Language currentLanguage) {
        final var languages = Language.values();
        final var builder = InlineKeyboardBuilder.builder();
        for (int i = 0; i < languages.length; ++i) {
            if (i % 5 == 0) {
                builder.addRow();
            }
            final String text;
            if (currentLanguage == languages[i]) {
                text = ":white_check_mark:" + languages[i].value();
            } else {
                text = languages[i].value();
            }
            builder.addButton(
                text,
                "%s%s%d".formatted(CommandText.SELECT_LANGUAGE, CommandText.CALLBACK_DELIMITER, languages[i].id())
            );
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup joinEventKeyboard(Language language, long chatEventId) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                Localization.get(language).joinEvent(),
                "%s%s%d".formatted(CommandText.JOIN_EVENT, CommandText.CALLBACK_DELIMITER, chatEventId)
            )
            .build();
    }
}
