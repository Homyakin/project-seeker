package ru.homyakin.seeker.telegram.utils;

import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.telegram.command.CommandType;

public class ReplyKeyboards {
    public static ReplyKeyboardMarkup levelUpKeyboard() {
        final var keyboard = ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(KeyboardButton.builder().text(EmojiParser.parseToUnicode(CommandType.UP_STRENGTH.getText())).build())
            .addButton(KeyboardButton.builder().text(EmojiParser.parseToUnicode(CommandType.UP_AGILITY.getText())).build())
            .addButton(KeyboardButton.builder().text(EmojiParser.parseToUnicode(CommandType.UP_WISDOM.getText())).build())
            .build();
        keyboard.setOneTimeKeyboard(true);
        return keyboard;
    }

    public static ReplyKeyboardMarkup mainKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(KeyboardButton.builder().text(
                EmojiParser.parseToUnicode(MenuLocalization.profileButton(language))
            ).build())
            .addRow()
            .addButton(KeyboardButton.builder().text(
                EmojiParser.parseToUnicode(MenuLocalization.languageButton(language))
            ).build())
            .build();
    }
}
