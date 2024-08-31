package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.locale.personal.MenuLocalization;

public class ReplyKeyboards {

    public static ReplyKeyboardMarkup mainKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(button(MenuLocalization.profileButton(language)))
            .addButton(button(MenuLocalization.inventoryButton(language)))
            .addRow()
            .addButton(button(MenuLocalization.bulletinBoardButton(language)))
            .addButton(button(MenuLocalization.shopButton(language)))
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
            .addButton(button(MenuLocalization.changeNameButton(language)))
            .addButton(button(MenuLocalization.showBadgesButton(language)))
            .addRow()
            .addButton(button(MenuLocalization.backButton(language)))
            .build();
    }

    public static ReplyKeyboardMarkup initChangeNameKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(button(ChangeNameLocalization.cancelButton(language)))
            .build();
    }

    public static ReplyKeyboardMarkup confirmChangeNameKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(button(ChangeNameLocalization.repeatButton(language)))
            .addButton(button(ChangeNameLocalization.confirmButton(language)))
            .addRow()
            .addButton(button(ChangeNameLocalization.cancelButton(language)))
            .build();
    }

    public static ReplyKeyboardMarkup bulletinBoardKeyboard(Language language) {
        return ReplyKeyboardBuilder.builder()
            .addRow()
            .addButton(button(MenuLocalization.takePersonalQuestButton(language)))
            .addRow()
            .addButton(button(MenuLocalization.backButton(language)))
            .build();
    }

    private static KeyboardButton button(String text) {
        return KeyboardButton.builder().text(text).build();
    }
}
