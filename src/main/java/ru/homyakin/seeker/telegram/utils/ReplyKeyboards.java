package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.MenuLocalization;

public class ReplyKeyboards {

    public static ReplyKeyboardMarkup mainKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(button(MenuLocalization.profileButton(language)))
            .addRow()
            .addButton(button(MenuLocalization.receptionDeskButton(language)))
            .build();
    }

    public static ReplyKeyboardMarkup receptionDeskKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(button(MenuLocalization.resetStatsButton(language)))
            .addButton(button(MenuLocalization.languageButton(language)))
            .addRow()
            .addButton(button(MenuLocalization.backButton(language)))
            .build();
    }

    private static KeyboardButton button(String text) {
        return KeyboardButton.builder().text(EmojiParser.parseToUnicode(text)).build();
    }
}
