package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public class InlineKeyboardBuilder {
    private static final String NOOP_CALLBACK = CommandType.NOOP.getText();
    private static final String PREV_PAGE_BUTTON = "◀";
    private static final String NEXT_PAGE_BUTTON = "▶";

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
        return addButton(text, callbackData, null);
    }

    public InlineKeyboardBuilder addButton(String text, String callbackData, InlineButtonStyle style) {
        if (row == null) {
            throw new IllegalStateException("Any row doesn't exist");
        }
        final var builder = InlineKeyboardButton
            .builder()
            .callbackData(callbackData)
            .text(text);
        if (style != null) {
            builder.style(style.value());
        }
        row.add(builder.build());
        return this;
    }

    public InlineKeyboardBuilder addUrlButton(String text, String url) {
        if (row == null) {
            throw new IllegalStateException("Any row doesn't exist");
        }
        row.add(
            InlineKeyboardButton
                .builder()
                .url(url)
                .text(text)
                .build()
        );
        return this;
    }

    /**
     * Adds ◀ | current/total | ▶ when {@code totalPages > 1}. Pages are 0-based.
     * Edge arrows and the page label use {@link #NOOP_CALLBACK}.
     */
    public InlineKeyboardBuilder addPaginationRow(int page, int totalPages, IntFunction<String> callbackForPage) {
        if (totalPages <= 1) {
            return this;
        }
        addRow();
        addButton(
            PREV_PAGE_BUTTON,
            page > 0 ? callbackForPage.apply(page - 1) : NOOP_CALLBACK
        );
        addButton((page + 1) + "/" + totalPages, NOOP_CALLBACK);
        addButton(
            NEXT_PAGE_BUTTON,
            page < totalPages - 1 ? callbackForPage.apply(page + 1) : NOOP_CALLBACK
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
