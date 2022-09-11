package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.homyakin.seeker.command.CommandText;
import ru.homyakin.seeker.locale.Language;

public class Keyboards {
    public static InlineKeyboardMarkup languageKeyboard(Language currentLanguage) {
        final var languages = Language.values();
        final var builder = InlineKeyboardBuilder.builder();
        for (int i = 0; i < languages.length; ++i) {
            if (i % 5 == 0) {
                builder.addRow();
            }
            final var buttonBuilder = InlineKeyboardButton
                .builder()
                .callbackData("%s%s%d".formatted(CommandText.SELECT_LANGUAGE, CommandText.CALLBACK_DELIMITER, languages[i].id()));
            if (currentLanguage == languages[i]) {
                buttonBuilder.text("+" + languages[i].value());
            } else {
                buttonBuilder.text(languages[i].value());
            }
            builder.addButton(
                buttonBuilder.build()
            );
        }
        return builder.build();
    }
}
