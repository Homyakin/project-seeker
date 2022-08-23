package ru.homyakin.seeker.command.language;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.homyakin.seeker.command.CommandText;
import ru.homyakin.seeker.models.Language;
import ru.homyakin.seeker.telegram.utils.InlineKeyboardBuilder;

class Utils {
    public static InlineKeyboardMarkup languageKeyboard(Language currentLanguage) {
        final var languages = Language.values();
        final var builder = InlineKeyboardBuilder.builder();
        for (int i = 0; i < languages.length; ++i) {
            if (i % 5 == 0) {
                builder.addRow();
            }
            final var buttonBuilder = InlineKeyboardButton
                .builder()
                .callbackData("%s_%d".formatted(CommandText.SELECT_LANGUAGE.text(), languages[i].id()));
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
