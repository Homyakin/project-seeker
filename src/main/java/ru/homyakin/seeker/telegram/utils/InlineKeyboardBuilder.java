package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardBuilder {
    private final ArrayList<List<InlineKeyboardButton>> rows = new ArrayList<>();
    private List<InlineKeyboardButton> row = null;

    public static InlineKeyboardBuilder builder() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardBuilder addRow() {
        if (row == null) {
            row = new ArrayList<>();
            return this;
        }
        if (row.size() == 0) {
            throw new IllegalStateException("Previous row is empty");
        }
        rows.add(row);
        row = new ArrayList<>();
        return this;
    }

    public InlineKeyboardBuilder addButton(String text, String callbackData) {
        if (row == null) {
            throw new IllegalStateException("Any row doesn't exist");
        }
        row.add(
            InlineKeyboardButton
                .builder()
                .callbackData(callbackData)
                .text(text)
                .build()
        );
        return this;
    }

    public InlineKeyboardMarkup build() {
        if (row == null || row.size() == 0) {
            throw new IllegalStateException("Last row is empty or doesn't exist");
        }
        rows.add(row);
        var markupInline = new InlineKeyboardMarkup();
        markupInline.setKeyboard(rows);
        return markupInline;
    }
}
