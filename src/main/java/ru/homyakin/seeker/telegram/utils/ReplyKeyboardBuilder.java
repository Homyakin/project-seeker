package ru.homyakin.seeker.telegram.utils;

import java.util.ArrayList;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class ReplyKeyboardBuilder {
    private final ArrayList<KeyboardRow> rows = new ArrayList<>();
    private KeyboardRow row = null;
    private boolean resize = true;

    public static ReplyKeyboardBuilder builder() {
        return new ReplyKeyboardBuilder();
    }

    public ReplyKeyboardBuilder addRow() {
        if (row == null) {
            row = new KeyboardRow();
            return this;
        }
        if (row.isEmpty()) {
            throw new IllegalStateException("Previous row is empty");
        }
        rows.add(row);
        row = new KeyboardRow();
        return this;
    }

    public ReplyKeyboardBuilder addButton(KeyboardButton button) {
        if (row == null) {
            throw new IllegalStateException("Any row doesn't exist");
        }
        row.add(button);
        return this;
    }

    public ReplyKeyboardBuilder changeResize() {
        resize = !resize;
        return this;
    }

    public ReplyKeyboardMarkup build() {
        if (row == null || row.isEmpty()) {
            throw new IllegalStateException("Last row is empty or doesn't exist");
        }
        rows.add(row);
        final var markupReply = new ReplyKeyboardMarkup(rows);
        markupReply.setResizeKeyboard(resize);
        return markupReply;
    }
}
