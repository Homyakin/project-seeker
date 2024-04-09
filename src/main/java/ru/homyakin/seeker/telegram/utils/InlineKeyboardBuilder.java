package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public class InlineKeyboardBuilder {
    private final List<InlineKeyboardRow> rows = new ArrayList<>();
    private InlineKeyboardRow row = null;

    public static InlineKeyboardBuilder builder() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardBuilder addRow() {
        if (row == null) {
            row = new InlineKeyboardRow();
            return this;
        }
        if (row.isEmpty()) {
            throw new IllegalStateException("Previous row is empty");
        }
        rows.add(row);
        row = new InlineKeyboardRow();
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
        if (row == null || row.isEmpty()) {
            throw new IllegalStateException("Last row is empty or doesn't exist");
        }
        rows.add(row);
        return new InlineKeyboardMarkup(rows);
    }
}
